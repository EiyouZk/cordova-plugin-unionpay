cordova.define("cordova-plugin-unionpay.unionpay", function(require, exports, module) {
/*global cordova, module*/

module.exports = {
    pay: function (paymentInfo, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "UnionPay", "pay", [paymentInfo]);
    }
};

});
