// Make element ids by iterating through alphabet.  Doesn't use 'z', so any
// other element ids can use a 'z' to be sure they won't conflict.
define(function () {
   var currElemId = [];
   var currElemIdLen = 1;
   currElemId[0] = 64;
   return function() {
      currElemId[0]++;
      for (var i=0; currElemId[i] == 90; i++) {
         currElemId[i] = 65;
         if (i == currElemIdLen - 1) {
            currElemId[currElemIdLen++] = 65;
            break;
         } else {
            currElemId[i+1]++;
         }
      }
      // FixMe: [performance] Is there a way to convert array of chars to
      // string in one go?
      var ret = [];
      for (var i=0; i<currElemIdLen; i++) {
         ret[i] = String.fromCharCode(currElemId[i]);
      }
      return ret.join('');
   }
});
