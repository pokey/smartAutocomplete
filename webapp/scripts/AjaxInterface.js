define(["jquery", "Exec", "Token"], function ($, Exec, Token) {
  function AjaxInterface() { }

  AjaxInterface.prototype = {
    getExecs : function(execs) {
      var jsonPath = "/json" + window.location.pathname.substring(5) + window.location.search;
      $.ajax({
        url: jsonPath,
        dataType: 'json',
        success: function( data ) {
          var len = data.length;
          for (i=0; i<len; i++) {
            var execInfo = data[i];
            var codeInfos = execInfo[3];
            var codeInfosLen = codeInfos.length;
            var hasCandidates = (execInfo[0] == "candidates");
            var exec = new Exec(execInfo[1], execInfo[2]);
            execs[i] = exec;
            var tokens = exec.tokens;
            var tokensCounter = 0;
            for (j=0; j<codeInfosLen; j++) {
              var datum = codeInfos[j];
              if (!hasCandidates) {
                var entropyStr = datum[2];
                var oov = entropyStr == "oov";
                var oracle = !(oov || entropyStr == "offBeam");
                var entropy = oracle ? parseFloat(entropyStr) : NaN;
                var token = new Token(datum[0], (datum[1] == "1" ? true : false),
                                      oov, oracle, entropy, datum[3], 0);
              } else {
                if (!datum.isActualToken) continue;
                var token = new Token(datum.loc, datum.ident, datum.oov,
                                      datum.oracle, datum.entropy,
                                      datum.oracle ? 1/datum.rank : 0, datum);
              }
              tokens[tokensCounter] = token;
              tokensCounter++;
            }
          }
          Exec.setBaseline();
          execs[0].initDisplay();
        },
        error: function(data, textStatus, errorThrown) {
          console.log(textStatus);
          console.log(errorThrown);
        }
      });
    },
  }

  return AjaxInterface;
});
