AUI().ready(

	/*
	This function gets loaded when all the HTML, not including the portlets, is
	loaded.
	*/

	function() {
	}
);

Liferay.Portlet.ready(

	/*
	This function gets loaded after each and every portlet on the page.

	portletId: the current portlet's id
	node: the Alloy Node object of the current portlet
	*/

	function(portletId, node) {
	}
);

Liferay.on(
	'allPortletsReady',

	/*
	This function gets loaded when everything, including the portlets, is on
	the page.
	*/

	function() {
	}
);



!function( $ ){

  "use strict"

 /* DROPDOWN CLASS DEFINITION
  * ========================= */

  var toggle = '[data-toggle="dropdown"]'
    , Dropdown = function ( element ) {
        var $el = $(element).on('click.dropdown.data-api', this.toggle);
        $('html').on('click.dropdown.data-api', function () {
          $el.parent().removeClass('open');
        });
      };

  Dropdown.prototype = {

    constructor: Dropdown

  , toggle: function ( e ) {
      var $this = $(this)
        , selector = $this.attr('data-target')
        , $parent
        , isActive;

      if (!selector) {
        selector = $this.attr('href');
        selector = selector && selector.replace(/.*(?=#[^\s]*$)/, ''); //strip for ie7
      }

      $parent = $(selector);
      $parent.length || ($parent = $this.parent());

      isActive = $parent.hasClass('open');

      clearMenus();
      !isActive && $parent.toggleClass('open');

      return false;
    }

  };

  function clearMenus() {
    $(toggle).parent().removeClass('open');
  }


  /* DROPDOWN PLUGIN DEFINITION
   * ========================== */

  $.fn.dropdown = function ( option ) {
    return this.each(function () {
      var $this = $(this)
        , data = $this.data('dropdown');
      if (!data) $this.data('dropdown', (data = new Dropdown(this)));
      if (typeof option == 'string') data[option].call($this);
    });
  };

  $.fn.dropdown.Constructor = Dropdown;


  /* APPLY TO STANDARD DROPDOWN ELEMENTS
   * =================================== */

  $(function () {
    $('html').on('click.dropdown.data-api', clearMenus);
    $('body').on('click.dropdown.data-api', toggle, Dropdown.prototype.toggle);
  });

}( window.jQuery );
