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
			
			
	code=sys.argv[1]
	sdate=sys.argv[2]

	df = ts.get_tick_data(code,date=sdate,src='tt')
	#转换中文
	for indexs in df.index:
	        df.loc[indexs, 'type'] = transalte(str(df["type"][indexs]));
			#df.loc[indexs, 'id'] = str(df.index);
			#print(str(indexs))
	#print(df)()
	
	columns = df.columns.values.tolist();
	for row in df.itertuples():
		line = '';
		for colName in columns:
			line = line+','+str(getattr(row, colName));
		print(line)
	
except BaseException as e:
	print('except:', e)

