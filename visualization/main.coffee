jQuery(document).ready ($) ->
	
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
	svg = d3.select('body').append('svg')
		.attr('width', width + margin.left + margin.right)
		.attr('height', height + margin.top + margin.bottom)
		.append('g').attr('transform', "translate(#{margin.left}, #{margin.top})")


	# get data and show chart
	$.get 'data/part-00000', {}, (data) ->
		data = JSON.parse data
		console.log 'Before', data.length
		data = data.filter (d) -> d.date? and d.close? and d.sentiment? and d.sentiment isnt 'NaN'
		console.log 'After', data.length
		
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
		svg.append('path').attr('d', line_stocks(data))
		# add sentiment line
		svg.append('path').style('stroke', 'red').attr('d', line_sentiment(data))

		# create X axis
		svg.append('g').attr('class', 'x axis').attr('transform', "translate(0, #{height})").call(x_axis)

		# create Y1 axis (stocks)
		svg.append('g').attr('class', 'y1 axis').call(y1_axis).append('text').attr('transform', 'rotate(-90)').attr('y', 6)
			.attr('dy', '.71em').style('text-anchor', 'end').text('Stock price')

		# create Y2 axis (sentiment)
		svg.append('g').attr('class', 'y2 axis').attr('transform', "translate(#{width}, 0)").style('fill', 'red').call(y2_axis)
			.append('text').attr('transform', 'rotate(-90)').attr('y', 6)
			.attr('dy', '.71em').style('text-anchor', 'end').text('Sentiment')

