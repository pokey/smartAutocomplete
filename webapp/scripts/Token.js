var showFeatureInfo;

define(["jquery", "color", "elemId", "text!/templates/tokenInfo.hbs",
       "text!/templates/candidate.hbs", "text!/templates/featureWeights.hbs",
       "text!/templates/kneserNey.hbs"],
       function ($, color, getElemId, tokenInfoTemplateText,
                 candidateTemplateText, featureWeightsTemplateText,
                 kneserNeyTemplateText) {
  var candidateTemplate = Handlebars.compile(candidateTemplateText);
  var tokenInfoTemplate = Handlebars.compile(tokenInfoTemplateText);
  var featureWeightsTemplate = Handlebars.compile(featureWeightsTemplateText);
  var kneserNeyTemplate = Handlebars.compile(kneserNeyTemplateText);
  var ENTROPY = 0;
  var RR = 1;

  var displayMode = ENTROPY;
  var onlyIdents = true;

  Handlebars.registerHelper('candidate', function(item, options) {
    return new Handlebars.SafeString(candidateTemplate(item));
  });

  Handlebars.registerHelper('featureWeights', function(item, options) {
    return new Handlebars.SafeString(featureWeightsTemplate(item));
  });

  Handlebars.registerHelper('num', function(x, options) {
    if(Math.abs(x - Math.floor(x)) < 1e-40) // An integer (probably)
      return ""+Math.floor(x);
    if(Math.abs(x) < 1e-3) // Scientific notation (close to 0)
      return x.toExponential();;
    return ""+Math.round(x*100)/100;
  });

  function Token(loc, isIdent, oov, oracle, entropy, reciprocalRank, extra) {
    this.loc = loc;
    this.isIdent = isIdent;
    this.oov = oov;
    this.oracle = oracle;
    this.entropy = entropy;
    this.reciprocalRank = reciprocalRank;
    this.$elem = $('#zt' + this.loc);
    this.extra = extra;
  }

  var selected = 0;

  function getColor(entropy, reciprocalRank, baselineToken) {
    if (displayMode == ENTROPY) {
      if (!baselineToken || entropy < baselineToken.entropy) {
        var c = color.toCssStr(color.transform(color.newPink, 0, entropy / 10, 1));
      } else {
        var scale = entropy / 10;
        var c =
          color.toCssStr(color.transform(color.yellow, 0, scale, 1));
      }
    } else {
      var c = color.toCssStr(color.transform(color.newPink, 0, 1-reciprocalRank, 1));
    }
    // var val = (entropy / 20 * 192) + 63;
    // var val = entropy / 20 * 255;
    // $('body').append('val = ' + val + ', entropy = ' + entropy);
    // var cVal = (Math.floor(val) << 16) + Math.floor(255-val);
    // var c = color.toCssStr(color.transform(cVal, 0, .25+(entropy - Math.floor(entropy))*.75,
    // .75+(entropy/5 - Math.floor(entropy/5))*.25));
    return c;
  }

  function deselect(token, clear) {
    if (clear) $("#ztokenInfo").html("");
    token.$elem.removeClass("hovered");
  }

  var candidateList;
  var featureName = 0;

  Handlebars.registerHelper('featureInfo', function(candidate, options) {
    if (featureName == 0) return "";
    return new Handlebars.SafeString(kneserNeyTemplate(candidate.featureInfos[featureName]));
  });

  showFeatureInfo = function(name) {
    if (name == featureName) {
      var empty = true;
      featureName = 0;
    } else {
      var empty = false;
      featureName = name;
    }
    var len = candidateList.length;
    for (var i=len; i--;) {
      var candidate = candidateList[i];
      var html = empty ? "" : kneserNeyTemplate(candidate.featureInfos[name]);
      $('#' + candidate.featureInfoId).html(html);
    }
  }

  function initCandidate(candidate, rank, entropy) {
    candidate.entropy = Math.round(entropy*100)/100
    candidate.color = getColor(entropy, 1/rank, 0);
    candidate.rank = rank;
    candidate.featureInfoId = getElemId();
  }

  function select(token) {
    candidateList = [];
    var extra = token.extra;
    var trueToken = extra.trueToken;
    initCandidate(trueToken, token.extra.rank, token.entropy);
    candidateList[0] = trueToken;

    var candidates = extra.candidates;
    var len = candidates.length;
    for (var i=len; i--;) {
      var candidate = candidates[i];
      candidateList[i+1] = candidate;
      initCandidate(candidate, i+1, -Math.log(candidate.prob));
    }

    $("#ztokenInfo").html(tokenInfoTemplate(token.extra));
    token.$elem.addClass("hovered");
  }

  Token.prototype = {
    initDisplay : function(baselineToken) {
      var $elem = this.$elem;
      var self = this;
      if (this.extra) {
        $elem.click(
          function() {
            if (selected) deselect(selected);
            select(self);
            selected = self;
          }
        );
        $elem.hover(
          function() {
            if (selected) deselect(selected);
            select(self);
          },
          function() {
            deselect(self, !selected);
            if (selected) select(selected);
          }
        );
      }
      this.updateDisplay(baselineToken);
    },

    updateDisplay : function(baselineToken) {
      var $elem = this.$elem;
      var title = "entropy=" + (Math.round(this.entropy*100)/100) +
                  ", rank=" + (!this.oracle ? "off beam" :
                               Math.round(1/this.reciprocalRank))
      this.$elem.attr('title', title);
      if (!this.oracle) {
        if (this.oov) {
          $elem.addClass('oov');
        } else {
          $elem.addClass('offBeam');
        }
      } else if (onlyIdents && !this.isIdent) {
        $elem.addClass('notIdent');
      } else {
        $elem.css('color', getColor(this.entropy, this.reciprocalRank,
                                    baselineToken));
      }
      if (this.reciprocalRank > .9999) {
        $elem.addClass('topPick');
        $elem.removeClass('baselineTopPick');
      } else {
        $elem.removeClass('topPick');
        if (baselineToken && baselineToken.reciprocalRank > .9999) {
          $elem.addClass('baselineTopPick');
        }
      }
    },
  };

  return Token;
});

