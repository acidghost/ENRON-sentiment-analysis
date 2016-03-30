# -*- coding: utf-8 -*-

import pandas as pd
import matplotlib.pyplot as plt
import statsmodels.api as sm
import numpy as np
import matplotlib
import glob
matplotlib.rc('pdf', fonttype=42)
matplotlib.rcParams.update({'font.size': 12})


def plot(df, custodian, hist):
	print " plot: " + custodian,
	save_path = "paper/imgs/sentiment_vs_stock_" + custodian

	## PLOT
	fig = plt.figure(figsize=(6, 4))
	ax = df.stock.plot(legend=True)
	ax.set_ylabel('stock price [$]')
	df.sentiment.plot(secondary_y=True, style='g', legend=True, mark_right=False)
	ax.right_ax.set_ylabel('sentiment')
	ax.right_ax.set_ylim(1,3)
	# plt.legend(loc='lower left', fontsize="small", borderpad=0.5)
	# plt.legend()
	if custodian != "overall":
		plt.title(custodian)
	plt.tight_layout()
	plt.savefig(save_path + ".pdf")

	## HIST
	if hist != False:
		fig = plt.figure(figsize=(6, 4))
		ax = df.sentiment.plot(kind="hist")
		ax.set_xlabel('sentiment')
		if custodian != "overall":
			plt.title(custodian)
		plt.tight_layout()
		plt.savefig(save_path + "_hist.pdf")

def plot_overall(df):
	save_path = "paper/imgs/sentiment_vs_stock_overall"

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
	plt.savefig(save_path + ".pdf")

	## HIST
	fig = plt.figure(figsize=(6, 4))
	ax = df.sentiment.plot(kind="hist")
	ax.set_xlabel('sentiment')
	plt.tight_layout()
	plt.savefig(save_path + "_hist.pdf")

def plot_info(df):
	print "\n*****************************"
	print df.describe()
	print "\n*****************************"
	print df.corr()

def data_loading(file_paths):
	df = pd.DataFrame()
	for file_path in glob.glob(file_paths):
		mailbox_id = file_path.split("/")[2].replace("sentiment_resume_","").replace(".json","")
		df_tmp = pd.read_json(file_path)
		df_tmp["custodian"] = pd.Series(len(df_tmp)*[mailbox_id], index=df_tmp.index)
		df = df.append(df_tmp)
	return df

def data_loading_overall(file_paths):	
	return pd.read_json(glob.glob(file_paths)[0]).set_index("date")

def pre_processing(df):
	df = df[["close", "sentiment", "custodian", "date"]]
	df=df.rename(columns = {'close':'stock'})
	df = df[df["date"] >= "1998-01-01"]
	df = df[df["date"] <= "2002-12-31"]
	return df

def pre_processing_overall(df, span_value):
	df = df.ix["1998-01-01":"2002-12-31"]
	df = df.sort_index()
	df = df[["close", "sentiment"]]
	df=df.rename(columns = {'close':'stock'})
	# df = df.resample("W").mean()
	df.stock = df.stock.ewm(span=5).mean()
	df.sentiment = df.sentiment.ewm(span=span_value).mean() # smoothing with average window
	return df


## OVERALL STUFF
data_overall = data_loading_overall("visualization/data/part-00000")
data_overall = pre_processing_overall(df=data_overall, span_value=50)
plot_overall(data_overall)
# plot_info(data_overall)

## INDIVIDUAL STUFF
data_individual = data_loading("visualization/data/*.json/part-00000")
data_individual = pre_processing(df=data_individual)
corr_per_user = dict()
for custodian_id, df in data_individual.groupby("custodian"):
	df = df.sort("date")
	corr_per_user[custodian_id] = df.corr().loc["stock", "sentiment"]
	df = df.set_index("date")
	df = df[["stock", "sentiment"]]
	df.stock = df.stock.interpolate()
	df.sentiment = df.sentiment.ewm(span=100).mean()
	plot(df, custodian_id, False)

corr_per_user = pd.DataFrame.from_dict(corr_per_user, orient="index")
corr_per_user = corr_per_user.rename(columns = {0:'stock_sentiment_corr'})
corr_per_user = corr_per_user.sort_values("stock_sentiment_corr", ascending=False)
corr_per_user = corr_per_user.round(decimals=3)
corr_per_user = corr_per_user.reset_index()

print corr_per_user



# users_per_corr = dict()
# for corr_val, df in corr_per_user.groupby("stock_sentiment_corr"): 
# 	key = round(corr_val, 2)
# 	users_per_corr[key] = users_per_corr.get(key, ", ") + str(sorted(df["index"].values)).replace("[","").replace("]","").replace("'","")
# users_per_corr = pd.DataFrame.from_dict(users_per_corr, orient="index")
# users_per_corr = users_per_corr.sort_index(ascending=False)
# f = open('paper/corrleation_per_user_table.txt', 'w')
# pd.set_option('display.max_rows', -1)
# pd.set_option('display.max_columns', -1)
# pd.set_option('display.max_colwidth', -1)
# print users_per_corr.to_latex(buf=f)
# print pd.cut(users_per_corr, bins=5)#.to_latex()
