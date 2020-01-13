import tushare as ts
import pandas as pd
import sys

try:
	pd.set_option('display.width', 1000)  # 设置字符显示宽度
	pd.set_option('display.max_rows', None)  # 设置显示最大行

	def transalte(s):
		if('买盘' in s):
			return "B";
		elif('卖盘' in s):
			return "S";
		else:
			return "N";
			
			
	#print('sys.argv.length:', len(sys.argv))
	#print('sys.argv.list:', str(sys.argv))
	#print('argv one? :', sys.argv[1])

	#df = ts.get_tick_data(sys.argv[1],date='2018-12-12',src='tt')
	#json_str = sys.argv[1]
	#req = eval(json_str)


	code=sys.argv[1]
	sdate=sys.argv[2]
	edate=sys.argv[3]
	padj=sys.argv[4]
	pfreq=sys.argv[5]




	ts.set_token('f7b8fb50ce43ba5e6e3a45f9ff24539e13319b3ab5e7a1824d032cc6')
	df = ts.pro_bar(ts_code=code, start_date=sdate, end_date=edate, adj=padj, freq= pfreq)
	#df = ts.pro_bar(req['ts_code'],start_date=req['start_date'], end_date=req['end_date'],adj=req['adj'],freq= req['freq'])

	#df = ts.get_hist_data('600848')
	#for indexs in df.index:
	#        df.loc[indexs, 'type'] = transalte(str(df["type"][indexs]));
	columns = df.columns.values.tolist();
	#print(title)
	#print(df)
	for row in df.itertuples():
		line = '';
		for colName in columns:
			line = line+','+str(getattr(row, colName));
		print(line)
except BaseException as e:
	print('except:', e)

