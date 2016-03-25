import pandas as pd
import matplotlib.pyplot as plt
import statsmodels.api as sm
import numpy as np
import matplotlib

matplotlib.rc('pdf', fonttype=42)
matplotlib.rcParams.update({'font.size': 12})

data_path = "visualization/data/part-00000"

df = pd.read_json(data_path).set_index(['date'])

df = df.ix["2000-01-01":"2001-12-31"]
df = df.sort_index()

df = df.resample("W").mean()
df.close = df.close.interpolate(method='polynomial', order=4)
df.sentiment = df.sentiment.interpolate(method='polynomial', order=4)
df["stock"] = df.close


print df.corr()
print "**********"
print df.sentiment.describe()
df.to_json(data_path+"_post")

fig = plt.figure(figsize=(6, 4))
ax = df.stock.plot(legend=True)
ax.set_ylabel('stock price [$]')

df.sentiment.plot(secondary_y=True, style='g', legend=True, mark_right=False)
ax.right_ax.set_ylabel('sentiment')
ax.right_ax.set_ylim(1,3)


# plt.show()
# plt.legend(loc='lower left', fontsize="small", borderpad=0.5)
# plt.legend()
plt.tight_layout()
plt.savefig("paper/imgs/sentiment_vs_stock.pdf")

fig = plt.figure(figsize=(6, 4))
ax = df.sentiment.plot(kind="hist")
ax.set_xlabel('sentiment')
plt.tight_layout()
plt.savefig("paper/imgs/sentiment_dist_hist.pdf")
