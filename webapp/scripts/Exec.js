var execs = [];

define(["jquery", "text!/templates/execEntry.hbs"],
       function ($, execEntryTemplateText) {
  var execEntryTemplate = Handlebars.compile(execEntryTemplateText);

  $(document).ready(function() {
    $(document).keydown(function(event) {
      if (event.target instanceof HTMLInputElement) {
        // FixMe: [usability] Should this actually check:
        // event.target == document.body
        // FixMe: [usability] Will need to be able to use ESC in text fields
        // at some point
        return false;
      }
      if (event.which > 47 && event.which < 58) {
        var idx = event.which-48-1;
        var exec = execs[idx];
        if (event.shiftKey) {
          if (baseline == exec) {
            $('#exec' + baseline.id).removeClass('baseline');
            baseline = null;
            window.location.hash = "";
          } else {
            if (baseline != null) {
              $('#exec' + baseline.id).removeClass('baseline');
            }
            baseline = exec;
            window.location.hash = idx;
            $('#exec' + baseline.id).addClass('baseline');
          }
          selected.updateDisplay();
        } else {
          exec.updateDisplay();
        }
      }
    });
  });

  function Exec(id, features) {
    this.id = id;
    this.features = features;
    this.tokens = [];
    var idx = execs.length;
    execs[idx] = this;
    var context = {id: id, idx: idx, features: features}
    $("#execList").append(execEntryTemplate(context));
  }

  var selected = null;
  var baseline = null;
  Exec.setBaseline = function() {
    // var baselineStr = window.location.hash;
    // baseline = baselineStr == "" ? null : execs[parseInt(baselineStr)];
    if (execs.length > 1) {
      baseline = execs[1];
      $('#exec' + baseline.id).addClass('baseline');
    } else {
      baseline = null;
    }
  }

  Exec.prototype = {
    initDisplay : function() {
      $('#exec' + this.id).addClass('selected');
      selected = this;

      var tokens = this.tokens;
      var len = tokens.length;
      var baselineTokens = baseline ? baseline.tokens : 0;
      for (var i=0; i<len; i++) {
        tokens[i].initDisplay(baselineTokens ? baselineTokens[i] : 0);
      }
    },

    updateDisplay : function() {
      if (selected) {
        $('#exec' + selected.id).removeClass('selected');
      }
      $('#exec' + this.id).addClass('selected');
      selected = this;

      var tokens = this.tokens;
      var len = tokens.length;
      var baselineTokens = baseline ? baseline.tokens : 0;
      for (var i=0; i<len; i++) {
        tokens[i].updateDisplay(baselineTokens ? baselineTokens[i] : 0);
      }
    },
  }

  return Exec;
});
