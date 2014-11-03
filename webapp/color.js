define(function() {
   var color = {};

   color.pink = 0xe4454d;
   color.purple = 0x9D3473;
   color.darkBackground = 0x1B0416;

   function gammaToLinear(chan) {
      return 255*Math.exp(2.2*Math.log(chan/255));
   }

   function linearToGamma(chan) {
      return 255*Math.exp(.455*Math.log(chan/255));
   }

   // h is in degrees, s and v are scale factors
   color.transform = function(color, h, s, v) {
      var red = gammaToLinear((color & (0xFF0000)) >> 16);
      var green = gammaToLinear((color & (0x00FF00)) >> 8);
      var blue = gammaToLinear(color & (0x0000FF));
      var VSU = v*s*Math.cos(h*Math.PI/180);
      var VSW = v*s*Math.sin(h*Math.PI/180);

      var rotRed = (.299*v+.701*VSU+.168*VSW)*red
         + (.587*v-.587*VSU+.330*VSW)*green
         + (.114*v-.114*VSU-.497*VSW)*blue;
      var rotGreen = (.299*v-.299*VSU-.328*VSW)*red
         + (.587*v+.413*VSU+.035*VSW)*green
         + (.114*v-.114*VSU+.292*VSW)*blue;
      var rotBlue = (.299*v-.3*VSU+1.25*VSW)*red
         + (.587*v-.588*VSU-1.05*VSW)*green
         + (.114*v+.886*VSU-.203*VSW)*blue;
      var retRed = linearToGamma(rotRed < 0 ? 0 : (rotRed > 255 ? 255 : rotRed));
      var retGreen = linearToGamma(rotGreen < 0 ? 0 : (rotGreen > 255 ? 255 :
                                                       rotGreen));
      var retBlue = linearToGamma(rotBlue < 0 ? 0 : (rotBlue > 255 ? 255 : rotBlue));
      return (Math.floor(retRed) << 16) + (Math.floor(retGreen) << 8) +
         Math.floor(retBlue);
   }
/*
   NamedConceptEditor.rotateColor = function(color, degrees) {
      var rotation = Math.PI*degrees/180;
      var red = gammaToLinear((color & (0xFF0000)) >> 16);
      var green = gammaToLinear((color & (0x00FF00)) >> 8);
      var blue = gammaToLinear(color & (0x0000FF));
      var colorY = .299*red+.587*green+.114*blue;
      var colorI = .596*red-.274*green-.321*blue;
      var colorQ = .211*red-.523*green+.311*blue;
      var conColorY = colorY;
      var cos = Math.cos(rotation);
      var sin = Math.sin(rotation);
      var conColorI = cos*colorI - sin*colorQ;
      var conColorQ = sin*colorI + cos*colorQ;
      var conRed = linearToGamma(conColorY+.956*conColorI+.621*conColorQ);
      var conGreen = linearToGamma(conColorY-.272*conColorI-.647*conColorQ);
      var conBlue = linearToGamma(conColorY-1.107*conColorI+1.705*conColorQ);
      return (Math.floor(conRed) << 16) + (Math.floor(conGreen) << 8) +
         Math.floor(conBlue);
   }
*/

   function zeroFill(number) {
      var str = number.toString(16);
      while (str.length < 2) {
         str = '0' + str;
      }
      return str;
   }
   color.toCssStr = function(color) {
      var red = (color >> 16) % 256;
      var green = (color >> 8) % 256;
      var blue = (color >> 0) % 256;
      return '#' + zeroFill(red) + zeroFill(green) + zeroFill(blue);
   }

   return color;
});
