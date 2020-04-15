package com.stable.utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.ApplicationFrame;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYDataset;

import com.stable.vo.ImageData;

public class ImageGeneratingUtil extends ApplicationFrame {
	private static final long serialVersionUID = 1L;
	private static final int width = 1024;
	private static final int height = 512;

	public static void generateImages(String filePath, List<ImageData> data) {
		new ImageGeneratingUtil(filePath, data);
	}

	private ImageGeneratingUtil(String filePath, List<ImageData> data) {
		super("");
		JFreeChart chart = createChart(data);
		ChartPanel panel = new ChartPanel(chart, true, true, true, false, true);
		panel.setPreferredSize(new java.awt.Dimension(width, height));
		setContentPane(panel);
		File file = new File(filePath);
		try {
			if (file.exists()) {
				file.delete();
			}
			ChartUtils.saveChartAsJPEG(file, chart, width, height);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Creates a chart.
	 *
	 * @return a chart.
	 */
	private JFreeChart createChart(List<ImageData> data) {

		XYDataset priceData = createPriceDataset(data);
//		XYDataset priceData = createVolumeDataset();
		String title = "";// Title
		JFreeChart chart = ChartFactory.createTimeSeriesChart(title, "", "", priceData, false, false, false);// Date,Price,第一个false，签名

		chart.setBorderStroke(new BasicStroke(0.0f));

		XYPlot plot = (XYPlot) chart.getPlot();
		NumberAxis rangeAxis1 = (NumberAxis) plot.getRangeAxis();
		rangeAxis1.setLowerMargin(0.00); // to leave room for volume bars
		DecimalFormat format = new DecimalFormat("00.00");
		rangeAxis1.setNumberFormatOverride(format);

		plot.setBackgroundPaint(Color.white);
//		plot.setOutlineVisible(false);
		plot.getDomainAxis().setVisible(false);// 控制底部列坐标是否显示
		plot.getRangeAxis().setVisible(false);// 控制左边列坐标是否显示
		plot.setOutlineVisible(true);

//		NumberAxis rangeAxis2 = new NumberAxis("");// Volume
//		rangeAxis2.setVisible(false);// 控制右边列坐标是否显示
//		// rangeAxis2.setUpperMargin(0.00); // to leave room for price line
////		plot.setRangeAxis(1, rangeAxis2);
//		plot.setDataset(1, createVolumeDataset());
//		plot.setRangeAxis(1, rangeAxis2);// 控制量是否显示？
//		plot.mapDatasetToRangeAxis(1, 1);// 控制价格和量一起显示
//		XYBarRenderer renderer2 = new XYBarRenderer(0.20);
//		plot.setRenderer(1, renderer2);
		return chart;

	}

	/**
	 * Creates a sample dataset.
	 */
	private XYDataset createPriceDataset(List<ImageData> data) {
		// create dataset 1...
		Day name = new Day();
		TimeSeries series1 = new TimeSeries(name);// ("Price", Day.class);
		for (ImageData d : data) {
			series1.add(new Day(d.getDate()), d.getNum());
		}
		TimeSeriesCollection ts = new TimeSeriesCollection(series1);
		return ts;

	}

//	private IntervalXYDataset createVolumeDataset() {
//
//		// create dataset 2...
//		TimeSeries series1 = new TimeSeries(new Day());// "Volume", Day.class);
//
//		return new TimeSeriesCollection(series1);
//
//	}

}
