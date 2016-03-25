import pandas as pd
import matplotlib.pyplot as plt
import statsmodels.api as sm
import numpy as np
# import matplotlib
# matplotlib.style.use('ggplot')

matplotlib.rc('pdf', fonttype=42)
matplotlib.rcParams.update({'font.size': 12})

data_path = "visualization/data/part-00000"

df = pd.read_json(data_path).set_index(['date'])

df = df.ix["2000-01-01":"2001-12-31"]
df = df.sort_index()

df = df.resample("W").mean()
df.close = df.close.interpolate(method='polynomial', order=4)
df.sentiment = df.sentiment.interpolate(method='polynomial', order=4)


print df
print df.corr()
df.to_json(data_path+"_postprocessed.json")

ax = df.close.plot()
ax.set_ylabel('stock price [$]')

df.sentiment.plot(secondary_y=True, style='g')
ax.right_ax.set_ylabel('sentiment')
ax.right_ax.set_ylim(1,3)


# plt.show()
fig.tight_layout(pad=0.4)
plt.savefig("paper/imgs/sentiment_vs_stock.pdf")