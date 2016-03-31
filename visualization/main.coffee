selector_to_datafile = (value) ->
	if value is "FULL"
		'data/part-00000'
	else
		"data/sentiment_resume_#{value}.json/part-00000"


show_chart = (selected_mailbox) ->
	$('#chart').html('')

	margin = 
		top: 20
		right: 80
		bottom: 30
		left: 50

	width = 960 - margin.left - margin.right
	height = 500 - margin.top - margin.bottom

	# time axis
	x = d3.time.scale().range([0, width])
	# stocks axis
	y1 = d3.scale.linear().range([height, 0])
	# sentiment axis
	y2 = d3.scale.linear().range([height, 0])

	x_axis = d3.svg.axis().scale(x).orient 'bottom'
	y1_axis = d3.svg.axis().scale(y1).orient 'left'
	y2_axis = d3.svg.axis().scale(y2).orient 'right'

	# create stocks line
	line_stocks = d3.svg.line()
		.x((d) -> x(d.date))
		.y((d) -> y1(d.close))
	# create sentiment line
	line_sentiment = d3.svg.line()
		.x((d) -> x(d.date))
		.y((d) -> y2(d.sentiment))

	# create SVG container element
	svg = d3.select('#chart').append('svg')
		.attr('width', width + margin.left + margin.right)
		.attr('height', height + margin.top + margin.bottom)
		.append('g').attr('transform', "translate(#{margin.left}, #{margin.top})")

	# get data and show chart
	$.get selector_to_datafile(selected_mailbox), {}, (data) ->
		data = JSON.parse data
		console.debug 'Before', data.length
		data = data.filter (d) -> d.date? and d.close? and d.sentiment? and d.sentiment isnt 'NaN'
		console.debug 'After', data.length
		
		data.sort (a, b) ->
			if a.date > b.date then 1
			else if a.date < b.date then -1
			else 0

		# preprocess data
		data.forEach (d) ->
			d.date = d3.time.format('%Y-%m-%d').parse(d.date)
			d.close = d.close
			d.sentiment = d.sentiment

		# set axes range
		x.domain(d3.extent(data, (d) -> d.date))
		y1.domain([0, d3.max(data, (d) -> d.close)])
		y2.domain([0, d3.max(data, (d) -> d.sentiment)])

		# add stocks line
		svg.append('path').style('stroke', 'steelblue').attr('d', line_stocks(data))
		# add sentiment line
		svg.append('path').style('stroke', 'red').attr('d', line_sentiment(data))

		# create X axis
		svg.append('g').attr('class', 'x axis')
			.attr('transform', "translate(0, #{height})").call(x_axis)

		# create Y1 axis (stocks)
		svg.append('g').attr('class', 'y1 axis').style('fill', 'steelblue').call(y1_axis)
			.append('text').attr('transform', 'rotate(-90)').attr('y', 6)
			.attr('dy', '.71em').style('text-anchor', 'end').text('Stock price')

		# create Y2 axis (sentiment)
		svg.append('g').attr('class', 'y2 axis').attr('transform', "translate(#{width}, 0)")
			.style('fill', 'red').call(y2_axis)
			.append('text').attr('transform', 'rotate(-90)').attr('y', 6)
			.attr('dy', '.71em').style('text-anchor', 'end').text('Sentiment')


show_hist = ->
	$('#corr-chart').html('')

	margin = 
		top: 20
		right: 80
		bottom: 30
		left: 50

	width = 960 - margin.left - margin.right
	height = 500 - margin.top - margin.bottom

	svg = d3.select('#corr-chart').append('svg')
		.attr('width', width + margin.left + margin.right)
		.attr('height', height + margin.top + margin.bottom)
		.append('g').attr('transform', "translate(#{margin.left}, #{margin.top})")

	$.get 'data/corr_per_user.json', {}, (data) ->

		# preprocess
		corrs = data.stock_sentiment_corr
		console.debug corrs
		names = (k for k, v of corrs)

		corrs_values = (corr for k, corr of corrs)
		console.debug corrs_values

		hist_data = ({ name: k, x: v, y: v } for k, v of corrs)
		# # hist = _.zip(hist, (v for k, v of data.points))
		# # 	.map((h, p) -> h.y = p; h)
		# console.debug hist


		# set x and y axes
		x = d3.scale.linear().domain([0, corrs_values.length]).range([0, width])
		y = d3.scale.linear().domain([d3.max(corrs_values) + d3.min(corrs_values), 0]).range([height, 0])
		x_axis = d3.svg.axis().scale(x).orient 'bottom'
		y_axis = d3.svg.axis().scale(y).orient 'left'


		# hist = d3.layout.histogram()
		# 	.value((d) -> d.x)
		# 	.frequency(true)(hist_data)
		# hist = _.zip(hist, names).map((h, n) -> h.name = n; h)
		hist = hist_data

		console.debug hist.length


		index = 0
		bar = svg.selectAll('.bar').data(hist)
			.enter().append('g')
			.attr('class', 'bar')
			.attr('transform', (d) -> "translate(#{x(index+=1)}, #{height/2})")

		index = 0
		bar.append('rect')
			.attr('x', (d) -> 1)
			.attr('y', (d) -> 0)
			.attr('width', (width / corrs_values.length)-1)
			.attr('height', (d) -> d3.max(corrs_values) - y(d.y))

		bar.append('text')
			.attr('dy', '.75em')
			.attr('y', 6)
			.attr('x', x(hist[0].x) / 2)
			.attr('text-anchor', 'middle')
			.style('fill', 'black')
			.text((d) -> d3.format(',.0f')(d.y) + "\n" + d.name)

		svg.append('g')
			.attr('class', 'x axis')
			.attr('transform', "translate(0, #{height})")
			.call(x_axis)

		svg.append('g')
			.attr('class', 'y axis')
			.attr('transform', "translate(#{width}, 0)")
			.call(y_axis)




jQuery(document).ready ($) ->

	mailboxes = null
	selected_mailbox = "FULL"
	mailbox_selector = $('#mailbox-selector')

	$.get 'data/mailboxes.txt', {}, (data) ->
		mailboxes = data.split('\n')
		options = mailboxes.map (mb) ->
			"<option value='#{mb}'>#{mb}</option>"
		mailbox_selector.html(options)


	show_hist()
	show_chart selected_mailbox

	mailbox_selector.on 'change', (evt) ->
		selected_mailbox = mailbox_selector.children(':selected').val()
		show_chart selected_mailbox

