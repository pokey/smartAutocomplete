require(["AjaxInterface", "drags"], function(AjaxInterface, _) {
  var ai = new AjaxInterface();
  var execs = [];
  ai.getExecs(execs);

  $('.controls').drags({cursor: "auto"});
});
