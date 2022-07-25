package com.stable.utils;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickMarkPosition;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.HighLowItemLabelGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.CandlestickRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.util.Args;
import org.jfree.data.time.Day;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;

import com.stable.vo.bus.TradeHistInfoDaliy;

/**
 *
 * @author cqiao
 */
public class CandlesTickChart {

	private static Logger logger = LogManager.getLogger(CandlesTickChart.class);
	private List<TradeHistInfoDaliy> bars;
	private static final int TIME_SCALE = 5;
	private static final double UP_OR_DOWN_RANGE = 0.1; // 设置price图上下界限比例
	private static final int UPPER_RANGE = 10;// 设置向上边框距离
	private static final long MILLISEC_IN_DAY = 24 * 60 * 60 * 1000;

	public static void strt(String filePath, String title, List<TradeHistInfoDaliy> bars, List<Date> allNonTradedays) {
//		Set<LocalDate> focusDates = new HashSet<>();
//		long startTime = System.currentTimeMillis();
		CandlesTickChart candlesTickChart = new CandlesTickChart();
		candlesTickChart.setBars(bars);
		JFreeChart chart = candlesTickChart.createCandlesTickChart(title, allNonTradedays);
		candlesTickChart.saveChartAsPNG(filePath, chart, 600, 450);

//		JFrame myFrame = new JFrame();
//		myFrame.setResizable(true);
//		myFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		myFrame.add(new ChartPanel(chart), BorderLayout.CENTER);
//		Toolkit kit = Toolkit.getDefaultToolkit();
//		Insets insets = kit.getScreenInsets(myFrame.getGraphicsConfiguration());
//		kit.getScreenSize();
//--这行注释//        myFrame.setSize((int) (screen.getWidth() - insets.left - insets.right), (int) (screen.getHeight() - insets.top - insets.bottom));
//		myFrame.setSize(600, 450);
//		myFrame.setLocation((int) (insets.left), (int) (insets.top));
//		myFrame.setVisible(true);
//		System.out.println("run time=" + (System.currentTimeMillis() - startTime));
	}

	/**
	 * K线图，带均线和成交量图
	 *
	 * @param title    K线图的标题
	 * @param tagDates 在该日期bar下方绘制一个向上的小三角进行标识
	 * @return
	 */
	public JFreeChart createCandlesTickChart(String title, List<Date> allNonTradedays) {
		try {
			OHLCSeriesCollection seriesCollection = new OHLCSeriesCollection();// 定义k线图数据集
			OHLCSeries seriesPriceUp = new OHLCSeries("price_up");// 高开低收数据序列，股票K线图的四个数据，依次是开，高，低，收
			OHLCSeries seriesPriceDown = new OHLCSeries("price_down");// 定义上涨和下跌的两个数据集
			TimeSeries seriesAllPrice = new TimeSeries("all_price");// 对应时间所有价格数据，用于计算均线

			TimeSeriesCollection volSeriesCollection = new TimeSeriesCollection();// 保留成交量数据的集合
			TimeSeries seriesVolumeUp = new TimeSeries("volume_up");// 对应时间成交量数据
			TimeSeries seriesVolumeDown = new TimeSeries("volume_down");// 对应时间成交量数据
			TimeSeries seriesAllVolume = new TimeSeries("all_volume");// 对应时间所有成交量数据，用于计算均线

//			TimeSeriesCollection tagSeriesCollection = new TimeSeriesCollection();
//			TimeSeries seriesTagUp = new TimeSeries("tag_up");// 买入标记数据
			double mLow = 0d;
			double mHigh = 0d;

//统计这段数据包含多少个交易日，好计算设置时间轴刻度规则

			Date startDate = DateUtil.parseDate(bars.get(bars.size() - 1).getDate());
			Date endDate = DateUtil.parseDate(bars.get(0).getDate());
			// 添加k线图数据,添加成交量数据
//			int barIndex = 0;
			for (TradeHistInfoDaliy bar : bars) {
				Date barDate = DateUtil.parseDate(bar.getDate());
				double open = bar.getOpen();
				double close = bar.getClosed();
				double high = bar.getHigh();
				double low = bar.getLow();
				double vol = bar.getVolume();
				Calendar quoteCalendar = Calendar.getInstance();
				quoteCalendar.setTimeInMillis(barDate.getTime());

				// 取这段交易日内最高和最低价格
				if (mHigh < high) {
					mHigh = high;
				}
				if (mLow > low) {
					mLow = low;
				} else if (mLow == 0) {
					mLow = low;
				}
				RegularTimePeriod day = new Day(quoteCalendar.get(Calendar.DAY_OF_MONTH),
						quoteCalendar.get(Calendar.MONTH) + 1, quoteCalendar.get(Calendar.YEAR));

//				if (tagDates.contains(openTime.toLocalDate())) {
//					RegularTimePeriod tagTime = new Minute(0, 12, openTime.getDayOfMonth(), openTime.getMonthValue(),
//							openTime.getYear());
//					seriesTagUp.add(tagTime, low * 0.95f);
//				}
				seriesAllPrice.add(day, close);
				seriesAllVolume.add(day, vol);
				if (open > close) {
					seriesPriceDown.add(day, open, high, low, close);
					seriesVolumeDown.add(day, vol);
				} else {
					seriesPriceUp.add(day, open, high, low, close);
					seriesVolumeUp.add(day, vol);
				}
			}

			// k线图数据
			seriesCollection.addSeries(seriesPriceUp);
			seriesCollection.addSeries(seriesPriceDown);
			// 成交量数据
			volSeriesCollection.addSeries(seriesVolumeUp);
			volSeriesCollection.addSeries(seriesVolumeDown);

			// 标记数据
//			tagSeriesCollection.addSeries(seriesTagUp);

			// 获取成收盘价均线图数据
			TimeSeriesCollection closeMaSeriesConllection = new TimeSeriesCollection();// 保留均线图数据的集合
			int[] maCounts = new int[] { 5, 10, 20, 30 };
			for (int i = 0; i < maCounts.length; ++i) {
//				TimeSeries seriesCloseMA = MovingAverage.createMovingAverage(seriesAllPrice,
//						"-MA" + String.valueOf(maCounts[i]), maCounts[i], 0);// 对应时间成交量数据,5天

				TimeSeries seriesCloseMA = CandlesTickChart.createMovingAverage(seriesAllPrice,
						"-MA" + String.valueOf(maCounts[i]), maCounts[i], 0);// 对应时间成交量数据,5天
																				// //MovingAverage.createMovingAverage
				closeMaSeriesConllection.addSeries(seriesCloseMA);
			}

			// 获取成交量均线图数据
			TimeSeriesCollection volMaSeriesConllection = new TimeSeriesCollection();// 保留均线图数据的集合
			TimeSeries seriesVolMA5 = CandlesTickChart.createMovingAverage(seriesAllVolume, "-MA5", 5, 0);// 对应时间成交量数据,5天
			TimeSeries seriesVolMa10 = CandlesTickChart.createMovingAverage(seriesAllVolume, "-MA10", 10, 0);// 对应时间成交量数据，10天
			TimeSeries seriesVolMa30 = CandlesTickChart.createMovingAverage(seriesAllVolume, "-MA30", 30, 0);// 对应时间成交量数据，30天

			volMaSeriesConllection.addSeries(seriesVolMA5);
			volMaSeriesConllection.addSeries(seriesVolMa10);
			volMaSeriesConllection.addSeries(seriesVolMa30);

			double widthGap = 0.5;
			// 设置K线图的画图器
			CandlestickRenderer candlestickRender = new CandlestickRenderer(CandlestickRenderer.WIDTHMETHOD_AVERAGE,
					true,
					new CustomHighLowItemLabelGenerator(new SimpleDateFormat("yyyy-MM-dd"), new DecimalFormat("0.00")));
			candlestickRender.setUpPaint(Color.gray);// 设置股票上涨的K线内部颜色
			candlestickRender.setDownPaint(Color.CYAN);// 设置股票下跌的K线内部颜色
			candlestickRender.setSeriesPaint(1, Color.CYAN);// 设置股票下跌的K线边框颜色
			candlestickRender.setSeriesPaint(0, Color.RED);// 设置股票上涨的K线边框颜色
			candlestickRender.setAutoWidthMethod(CandlestickRenderer.WIDTHMETHOD_AVERAGE);// 设置如何对K线图的宽度进行设定
			candlestickRender.setAutoWidthGap(widthGap);// 设置各个K线图之间的间隔
			candlestickRender.setSeriesVisibleInLegend(0, false);// 设置不显示legend（数据颜色提示)
			candlestickRender.setSeriesVisibleInLegend(1, false);// 设置不显示legend（数据颜色提示)

			// 设置k线图y轴参数
			NumberAxis y1Axis = new NumberAxis();// 设置Y轴，为数值,后面的设置，参考上面的y轴设置
			y1Axis.setAutoRange(false);// 设置不采用自动设置数据范围
			y1Axis.setUpperMargin(UPPER_RANGE);// 设置向上边框距离
			y1Axis.setLabelFont(new Font("微软雅黑", Font.BOLD, 12));
			y1Axis.setRange(mLow - (mLow * UP_OR_DOWN_RANGE), mHigh + (mLow * UP_OR_DOWN_RANGE));// 设置y轴数据范围

//        y1Axis.setAutoTickUnitSelection(true);//数据轴的数据标签是否自动确定（默认为true）
			// 设置k线图x轴，也就是时间轴
			DateAxis domainAxis = new DateAxis();
			domainAxis.setAutoRange(false);// 设置不采用自动设置时间范围
			int dayCount = bars.size();
			// 设置时间范围，注意，最大和最小时间设置时需要+ - 。否则时间刻度无法显示
			startDate.setTime(startDate.getTime() - MILLISEC_IN_DAY);
			endDate.setTime(endDate.getTime() + MILLISEC_IN_DAY);
			domainAxis.setRange(startDate, endDate);

			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			domainAxis.setAutoTickUnitSelection(false);// 设置不采用自动选择刻度值
			domainAxis.setTickMarkPosition(DateTickMarkPosition.START);// 设置标记的位置
			domainAxis.setStandardTickUnits(DateAxis.createStandardDateTickUnits());// 设置标准的时间刻度单位
			domainAxis.setTickUnit(new DateTickUnit(DateTickUnitType.DAY, dayCount / TIME_SCALE));// 设置时间刻度的间隔
			domainAxis.setDateFormatOverride(dateFormat);// 设置时间格式
			// 设置时间线显示,排除所有节假日
			SegmentedTimeline timeline = SegmentedTimeline.newMondayThroughFridayTimeline();
			for (Date holiday : allNonTradedays) {
				timeline.addException(holiday);
			}
			domainAxis.setTimeline(timeline);

			// 设置均线图画图器
			XYLineAndShapeRenderer lineAndShapeRenderer = new XYLineAndShapeRenderer();
			lineAndShapeRenderer.setDefaultItemLabelsVisible(true);
			lineAndShapeRenderer.setSeriesShapesVisible(0, false);// 设置不显示数据点模型
			lineAndShapeRenderer.setSeriesShapesVisible(1, false);
			lineAndShapeRenderer.setSeriesShapesVisible(2, false);
			lineAndShapeRenderer.setSeriesShapesVisible(3, false);// 设置不显示数据点模型
			lineAndShapeRenderer.setSeriesShapesVisible(4, false);
			lineAndShapeRenderer.setSeriesShapesVisible(5, false);
			lineAndShapeRenderer.setSeriesPaint(0, Color.WHITE);// 设置均线颜色
			lineAndShapeRenderer.setSeriesPaint(1, Color.YELLOW);
			lineAndShapeRenderer.setSeriesPaint(2, Color.MAGENTA);
			lineAndShapeRenderer.setSeriesPaint(3, Color.BLUE);
			lineAndShapeRenderer.setSeriesPaint(4, Color.GREEN);

			// 标记形状
//			Shape tagShap = ShapeUtils.createUpTriangle(6f);// the size factor (equal to half the height of the
			// tagShap).
//			XYItemRenderer tagRenderer = new XYShapeRenderer();
//			tagRenderer.setSeriesShape(0, tagShap);
//			tagRenderer.setSeriesPaint(0, Color.ORANGE);

			// 生成画图细节 第一个和最后一个参数这里需要设置为null，否则画板加载不同类型的数据时会有类型错误异常
			// 可能是因为初始化时，构造器内会把统一数据集合设置为传参的数据集类型，画图器可能也是同样一个道理
			XYPlot candlestickSubplot = new XYPlot(null, domainAxis, y1Axis, null);
			candlestickSubplot.setBackgroundPaint(BG);// 设置曲线图背景色
			candlestickSubplot.setDomainGridlinesVisible(false);// 不显示网格
//            plot.setRangeGridlinePaint(Color.RED);//设置间距格线颜色为红色
			candlestickSubplot.setRangeGridlinesVisible(false);
			// 将设置好的数据集合和画图器放入画板
			candlestickSubplot.setDataset(0, seriesCollection);
			candlestickSubplot.setRenderer(0, candlestickRender);
			candlestickSubplot.setDataset(1, closeMaSeriesConllection);
			candlestickSubplot.setRenderer(1, lineAndShapeRenderer);
//			candlestickSubplot.setDataset(2, tagSeriesCollection);
//			candlestickSubplot.setRenderer(2, tagRenderer);

			// 设置柱状图参数
			XYBarRenderer barRenderer = new XYBarRenderer();
			barRenderer.setDrawBarOutline(true);// 设置显示边框线
			barRenderer.setBarPainter(new StandardXYBarPainter());// 取消渐变效果
			barRenderer.setMargin(widthGap);// 设置柱形图之间的间隔
			barRenderer.setSeriesPaint(0, Color.GRAY);// 设置柱子内部颜色
			barRenderer.setSeriesPaint(1, Color.CYAN);// 设置柱子内部颜色
			barRenderer.setSeriesOutlinePaint(0, Color.RED);// 设置柱子边框颜色
			barRenderer.setSeriesOutlinePaint(1, Color.CYAN);// 设置柱子边框颜色
			barRenderer.setSeriesVisibleInLegend(0, false);// 设置不显示legend（数据颜色提示)
			barRenderer.setSeriesVisibleInLegend(1, false);// 设置不显示legend（数据颜色提示)

			barRenderer.setShadowVisible(false);// 设置没有阴影

			// 设置柱状图y轴参数
			NumberAxis y2Axis = new NumberAxis();// 设置Y轴，为数值,后面的设置，参考上面的y轴设置
			y2Axis.setLabelFont(new Font("微软雅黑", Font.BOLD, 12));// 设置y轴字体
			y2Axis.setAutoRange(true);// 设置采用自动设置时间范围
			y2Axis.setVisible(false);

			// 这里不设置x轴，x轴参数依照k线图x轴为模板
			XYPlot volumeSubplot = new XYPlot(volSeriesCollection, null, y2Axis, barRenderer);
			volumeSubplot.setBackgroundPaint(BG);// 设置曲线图背景色
			volumeSubplot.setDomainGridlinesVisible(false);// 不显示网格
//            plot2.setRangeGridlinePaint(Color.RED);//设置间距格线颜色为红色
			volumeSubplot.setRangeGridlinesVisible(false);
			volumeSubplot.setDataset(1, volMaSeriesConllection);
			volumeSubplot.setRenderer(1, lineAndShapeRenderer);
			// 建立一个恰当的联合图形区域对象，以x轴为共享轴
			CombinedDomainXYPlot domainXYPlot = new CombinedDomainXYPlot(domainAxis);//
			domainXYPlot.add(candlestickSubplot, 3);// 添加图形区域对象，后面的数字是计算这个区域对象应该占据多大的区域2/3
			domainXYPlot.add(volumeSubplot, 1);// 添加图形区域对象，后面的数字是计算这个区域对象应该占据多大的区域1/3
			domainXYPlot.setGap(5);// 设置两个图形区域对象之间的间隔空间

			// 生成图
			JFreeChart chart = new JFreeChart(title, new Font("微软雅黑", Font.BOLD, 12), domainXYPlot, false);
//            JFreeChart chart = new JFreeChart(title, new Font("微软雅黑", Font.BOLD, 12), domainXYPlot, true);
//            LegendTitle legend = chart.getLegend(); //设置图例太难看，超过3个会报错，所以去掉         
//            legend.setPosition(RectangleEdge.TOP);
			return chart;
		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
		}
		return null;
	}

	public static TimeSeries createMovingAverage(TimeSeries source, String name, int periodCount, int skip) {
		Args.nullNotPermitted(source, "source");
		if (periodCount < 1) {
			throw new IllegalArgumentException("periodCount must be greater " + "than or equal to 1.");
		}

		TimeSeries result = new TimeSeries(name);
		if (source.getItemCount() > skip) {
			for (int i = source.getItemCount() - 1; i >= periodCount; i--) {
				// get the current data item...
				RegularTimePeriod period = source.getTimePeriod(i);
				// work out the average for the earlier values...
				double sum = 0.0;
				int offset = 0;
				while (offset < periodCount) {
					TimeSeriesDataItem item = source.getDataItem(i - offset);
//					RegularTimePeriod p = item.getPeriod();
					item.getPeriod();
					Number v = item.getValue();
					if (v != null) {
						sum = sum + v.doubleValue();
					}
					offset = offset + 1;
				}
				result.add(period, sum / periodCount);
			}
		}
		return result;
	}

	private java.awt.Color BG = Color.BLACK;

	public void saveChartAsPNG(String filePath, JFreeChart chart, int width, int height) {
		File file = new File(filePath);
		try {
			if (!file.exists()) {
				file.createNewFile();
			}
			FileOutputStream output = new FileOutputStream(file);
			ChartUtils.writeChartAsPNG(output, chart, width, height);
			output.close();
		} catch (IOException ex) {
			logger.error(ex);
		}
	}

	public List<TradeHistInfoDaliy> getBars() {
		return bars;
	}

	public void setBars(List<TradeHistInfoDaliy> bars) {
		this.bars = bars;
	}
}

class CustomHighLowItemLabelGenerator extends HighLowItemLabelGenerator {
	private static final long serialVersionUID = 1L;

	/**
	 * The date formatter.
	 */
	private DateFormat dateFormatter;

	/**
	 * The number formatter.
	 */
	private NumberFormat numberFormatter;

	/**
	 * Creates a tool tip generator using the supplied date formatter.
	 *
	 * @param dateFormatter   the date formatter (<code>null</code> not permitted).
	 * @param numberFormatter the number formatter (<code>null</code> not
	 *                        permitted).
	 */
	public CustomHighLowItemLabelGenerator(DateFormat dateFormatter, NumberFormat numberFormatter) {
		if (dateFormatter == null) {
			throw new IllegalArgumentException("Null 'dateFormatter' argument.");
		}
		if (numberFormatter == null) {
			throw new IllegalArgumentException("Null 'numberFormatter' argument.");
		}
		this.dateFormatter = dateFormatter;
		this.numberFormatter = numberFormatter;
	}

	/**
	 * Generates a tooltip text item for a particular item within a series.
	 *
	 * @param dataset the dataset.
	 * @param series  the series (zero-based index).
	 * @param item    the item (zero-based index).
	 *
	 * @return The tooltip text.
	 */
	@Override
	public String generateToolTip(XYDataset dataset, int series, int item) {
		String result = "";
		if (dataset instanceof OHLCDataset) {
			OHLCDataset d = (OHLCDataset) dataset;
			Number high = d.getHigh(series, item);
			Number low = d.getLow(series, item);
			Number open = d.getOpen(series, item);
			Number close = d.getClose(series, item);
			Number x = d.getX(series, item);

//            result = d.getSeriesKey(series).toString();
			if (x != null) {
				Date date = new Date(x.longValue());
				result = result + "日期=" + this.dateFormatter.format(date);
				if (high != null) {
					result = result + ",高=" + this.numberFormatter.format(high.doubleValue());
				}
				if (low != null) {
					result = result + ",低=" + this.numberFormatter.format(low.doubleValue());
				}
				if (open != null) {
					result = result + ",开=" + this.numberFormatter.format(open.doubleValue());
				}
				if (close != null) {
					result = result + ",收=" + this.numberFormatter.format(close.doubleValue());
				}
			}
		}
		return result;
	}

}