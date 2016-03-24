class MainCtrl

	constructor: ->
		$.get 'data/part-r-00000-078a1441-bf0a-447b-a1ad-ff8c07a270a6', {}, (response) =>
			@data = response



jQuery(document).ready ($) ->
	main = new MainCtrl

