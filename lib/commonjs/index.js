"use strict";

Object.defineProperty(exports, "__esModule", {
  value: true
});
var _nativeWrapper = require("./native-wrapper");
Object.keys(_nativeWrapper).forEach(function (key) {
  if (key === "default" || key === "__esModule") return;
  if (key in exports && exports[key] === _nativeWrapper[key]) return;
  Object.defineProperty(exports, key, {
    enumerable: true,
    get: function () {
      return _nativeWrapper[key];
    }
  });
});
//# sourceMappingURL=index.js.map