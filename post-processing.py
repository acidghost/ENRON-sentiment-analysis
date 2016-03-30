import pandas as pd
import matplotlib.pyplot as plt
import statsmodels.api as sm
import numpy as np
import matplotlib
import glob
matplotlib.rc('pdf', fonttype=42)
matplotlib.rcParams.update({'font.size': 12})


def plot_overall(df):
	## PLOT
	fig = plt.figure(figsize=(6, 4))
	ax = df.stock.plot(legend=True)
	ax.set_ylabel('stock price [$]')
	df.sentiment.plot(secondary_y=True, style='g', legend=True, mark_right=False)
	ax.right_ax.set_ylabel('sentiment')
	ax.right_ax.set_ylim(1,3)
	# plt.legend(loc='lower left', fontsize="small", borderpad=0.5)
	# plt.legend()
	plt.tight_layout()
	plt.savefig("paper/imgs/sentiment_vs_stock.pdf")

	## HIST
	fig = plt.figure(figsize=(6, 4))
	ax = df.sentiment.plot(kind="hist")
	ax.set_xlabel('sentiment')
	plt.tight_layout()
	plt.savefig("paper/imgs/sentiment_dist_hist.pdf")

def plot_info(df):
	print "\n*****************************"
	print df.describe()
	print "\n*****************************"
	print df.corr()

def data_loading():
	file_paths = "visualization/data/*.json/part-00000"
	df = pd.DataFrame()
	for file_path in glob.glob(file_paths):
		mailbox_id = file_path.split("/")[2].replace("sentiment_resume_","").replace(".json","")
		df_tmp = pd.read_json(file_path)
		df_tmp["custodian"] = pd.Series(len(df_tmp)*[mailbox_id], index=df_tmp.index)
		df = df.append(df_tmp)
	return df

def pre_processing(df):
	df = df[["close", "sentiment", "custodian", "date"]]
	df=df.rename(columns = {'close':'stock'})
	df.sentiment = df.sentiment.ewm(span=2000).mean() # smoothing with average window
	df = df[df["date"] >= "1998-01-01"]
	df = df[df["date"] <= "2002-12-31"]
	return df


data = data_loading()
data = pre_processing(data)

## OVERALL STUFF
# plot_overall(data)
# plot_info(data)


# for custodian_id, df in data.groupby("custodian"): # 1=custodian, 0=date
# 	print df.corr()
	# print df.corr().loc["stock", "sentiment"]

