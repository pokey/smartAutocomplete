define(["jquery"], function ($) {
  var listeners = [];

  $.get( "/templates.html", function( data ) {
    $('body').append('<div style="display:none" id="templates">' + data + '</div>');
    for (var i=listeners.length;i--;) {
      listeners[i]();
    }
  });

  var template = function(name) {
    return Handlebars.compile($('#' + name).html());
  };

  template.addListener = function(listener) {
    listeners[listeners.length] = listener;
  }

  return template;
});
