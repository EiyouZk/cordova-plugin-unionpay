package org.unionpay.uppayplugin.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.text.TextUtils;
//import com.alipay.sdk.app.PayTask;
import com.unionpay.UPPayAssistEx;
//import com.unionpay.uppay.PayActivity;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import android.util.Log;
import android.os.Message;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;
import org.apache.cordova.PluginResult;

import org.unionpay.uppayplugin.demo.APKActivity;
import org.unionpay.uppayplugin.demo.BaseActivity;


public class UnionPayPlugin extends CordovaPlugin {
    private static String TAG = "AliPayPlugin";

    //商户PID
    private String partner = "";
    //商户收款账号
    private String seller = "";
    //商户私钥，pkcs8格式
    private String APPID = "2016083001824756";

    private String privateKey = "";

    protected CallbackContext currentCallbackContext;

    private BaseActivity Baseobj=new BaseActivity() {
        @Override
        public void doStartUnionPayPlugin(Activity activity, String tn, String mode) {
            // mMode参数解释：
            // 0 - 启动银联正式环境
            // 1 - 连接银联测试环境
            int ret = UPPayAssistEx.startPay(cordova.getActivity(), null, null, tn, mode);
            //UPPayAssistEx.startPayByJAR(cordova.getActivity(), PayActivity.class, null, null, tn,mode);

           if (ret == PLUGIN_NEED_UPGRADE || ret == PLUGIN_NOT_INSTALLED) {
              // 需要重新安装控件
               Log.e(LOG_TAG, " plugin not found or need upgrade!!!");

               AlertDialog.Builder builder = new AlertDialog.Builder(this);
               builder.setTitle("提示");
               builder.setMessage("完成购买需要安装银联支付控件，是否安装？");

             builder.setNegativeButton("确定",
                       new DialogInterface.OnClickListener() {
                           @Override
                           public void onClick(DialogInterface dialog, int which) {
                             UPPayAssistEx.installUPPayPlugin(cordova.getActivity());
                              dialog.dismiss();
                          }
                       });

               builder.setPositiveButton("取消",
                      new DialogInterface.OnClickListener() {

                           @Override
                           public void onClick(DialogInterface dialog, int which) {
                               dialog.dismiss();
                           }
                      });
               builder.create().show();

           }
            Log.e(LOG_TAG, "" + ret);

        }

        @Override
        public void updateTextView(TextView tv) {

        }

    };



    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        partner = webView.getPreferences().getString("partner", "");
        seller = webView.getPreferences().getString("seller", "");
        privateKey = webView.getPreferences().getString("privatekey", "");
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        try {
            JSONObject arguments = args.getJSONObject(0);
            //String tradeNo = arguments.getString("tradeNo");
            //String subject = arguments.getString("subject");
            //String body = arguments.getString("body");
            //String price = arguments.getString("price");
            //String payinfo = arguments.getString("payinfo");
            //String notifyUrl = "http://sys.oonline.sciencereading.cn/api/alipay/notify_url.php";
            String tn = arguments.getString("tn");
            String payinfo = tn;
            //callbackContext.error(0);
            this.pay("tradeNo", "subject", "body", payinfo, "notifyUrl", callbackContext);
        } catch (JSONException e) {
            callbackContext.error(new JSONObject());
            e.printStackTrace();
            return false;
        }
        return true;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*************************************************
         * 步骤3：处理银联手机支付控件返回的支付结果
         ************************************************/
        if (data == null) {
            return;
        }

        String msg = "";
        /*
         * 支付控件返回字符串:success、fail、cancel 分别代表支付成功，支付失败，支付取消
         */
        String str = data.getExtras().getString("pay_result");
        if (str.equalsIgnoreCase("success")) {
            // 支付成功后，extra中如果存在result_data，取出校验
            // result_data结构见c）result_data参数说明
            if (data.hasExtra("result_data")) {
                String result = data.getExtras().getString("result_data");
                try {
                    JSONObject resultJson = new JSONObject(result);
                    String sign = resultJson.getString("sign");
                    String dataOrg = resultJson.getString("data");
                    // 验签证书同后台验签证书
                    // 此处的verify，商户需送去商户后台做验签
                    boolean ret = verify(dataOrg, sign, "01");
                    if (ret) {
                        // 验证通过后，显示支付结果
                        msg = "支付成功！";
                        currentCallbackContext.success(makejson(result,0));
                        Toast.makeText(cordova.getActivity(),"支付成功", Toast.LENGTH_SHORT).show();


                    } else {
                        // 验证不通过后的处理
                        // 建议通过商户后台查询支付结果
                        msg = "支付失败！";
                        currentCallbackContext.success(makejson(result,2));
                        Toast.makeText(cordova.getActivity(),"支付失败", Toast.LENGTH_SHORT).show();

                    }
                } catch (JSONException e) {
                }
            } else {
                // 未收到签名信息
                // 建议通过商户后台查询支付结果
                msg = "支付成功！";
                currentCallbackContext.success(makejson("success",0));
                Toast.makeText(cordova.getActivity(),"支付成功", Toast.LENGTH_SHORT).show();
            }
        } else if (str.equalsIgnoreCase("fail")) {
            msg = "支付失败！";
            currentCallbackContext.error(makejson("fail",2));
            Toast.makeText(cordova.getActivity(),"支付失败", Toast.LENGTH_SHORT).show();
        } else if (str.equalsIgnoreCase("cancel")) {
            msg = "用户取消了支付";
            currentCallbackContext.error(makejson("cancel",1));
            Toast.makeText(cordova.getActivity(),"用户取消了支付", Toast.LENGTH_SHORT).show();
        }

//        AlertDialog.Builder builder = new AlertDialog.Builder(cordova.getActivity());
//        builder.setTitle("支付结果通知");
//        builder.setMessage(msg);
//        builder.setInverseBackgroundForced(true);
//        // builder.setCustomTitle();
//        builder.setNegativeButton("确定", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//            }
//        });
//        builder.create().show();
    }


    private boolean verify(String msg, String sign64, String mode) {
        // 此处的verify，商户需送去商户后台做验签
        return true;

    }

    public JSONObject makejson(String restr,int status){
        Map<String,String> resobj = new HashMap<String, String>();
        if (0 == status){
            resobj.put("resultStatus", "true");
            resobj.put("memo", "memo");
            resobj.put("result", "支付完成");
            resobj.put("resultdate",restr);
        }else if( 1 == status){
            resobj.put("resultStatus", "false");
            resobj.put("memo", "memo");
            resobj.put("result", "用户取消操作");
            resobj.put("resultdate",restr);
        }else if( 2 == status){
            resobj.put("resultStatus", "false");
            resobj.put("memo", "memo");
            resobj.put("result", "支付失败");
            resobj.put("resultdate",restr);
        }

        return new JSONObject(resobj);
    }


    public void pay(String tradeNo, String subject, String body, String in_payinfo, String notifyUrl, final CallbackContext callbackContext) {
        currentCallbackContext = callbackContext;

        Map<String, String> params = buildOrderParamMap("2016083001824756", tradeNo , partner, seller, subject, body, "0.01");
        String orderParam = buildOrderParam(params);
        String sign = getSign(params, privateKey);

        //final String payInfo = orderParam + "&" + sign;

        //final String payInfo = "app_id=2016083001824756&biz_content=%7B%22timeout_express%22%3A%2230m%22%2C%22seller_id%22%3A%22%22%2C%22product_code%22%3A%22QUICK_MSECURITY_PAY%22%2C%22total_amount%22%3A%220.01%22%2C%22subject%22%3A%22%5Cu63cf%5Cu8ff0%22%2C%22body%22%3A%22%5Cu51fa%5Cu7248%5Cu793e%22%2C%22out_trade_no%22%3A%22sale11320161202163414609%22%7D&charset=utf-8&format=json&method=alipay.trade.app.pay¬ify_url=http%3A%2F%2Fsys.sp.kf.gli.cn%2Fapi%2Falipay%2Fnotify_url.php×tamp=2016-12-02+16%3A34%3A14&version=1.0&sign=peBM%2BEhbFoetwQpDqefp6u9peFN%2BxL0xje2GLqhex7bc76%2FpexA8r2eiq2m2WnjoAFQCJutbzae5PdU4FYJVfbVbyYUtcAFk9OYSf3GCI67wLfW2t%2BtImmzJEBKFsMB0zFmPn1kMZbRwOVaB1rvI%2BZ7NdRdOopY601zFDegI55E%3D&sign_type=RSA";

        final String payInfo = in_payinfo;

        // 订单
        /*String orderInfo = createRequestParameters(subject, body, "0.01", tradeNo, notifyUrl);

        // 对订单做RSA 签名
        String sign = sign(orderInfo);
        try {
            // 仅需对sign 做URL编码
            sign = URLEncoder.encode(sign, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        // 完整的符合支付宝参数规范的订单信息
        final String payInfo = orderInfo + "&sign=\"" + sign + "\"&"
                + getSignType();*/


        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                //PayTask alipay = new PayTask(cordova.getActivity());
                //Map<String, String> result = alipay.payV2(payInfo, true);
                //Map<String , Object> obj = new HashMap<String, Object>();
                //obj.put("result" ,result);
                //obj.put("callback" ,callbackContext);
                //Log.i("msp", result.toString());

//                Message msg = new Message();
                //msg.what = 1;
                //msg.obj = obj;


//                Baseobj.run();
                Baseobj.startrun(payInfo);

                //msg.obj = Baseobj.getTN();
                //mHandler.sendMessage(msg);



                //mHandler.sendMessage(msg);

                // 构造PayTask 对象
                //PayTask alipay = new PayTask(cordova.getActivity());
                // 调用支付接口，获取支付结果
                //Map<String, String> result = alipay.payV2(payInfo, true);


                //Log.i("msp", result.toString());

                //Message msg = new Message();
                //msg.what = SDK_PAY_FLAG;
                //msg.obj = result;
               // mHandler.sendMessage(msg);
                //String result = alipay.payV2(payInfo,true);

                //PayResult payResult = new PayResult(result);
                /*if (TextUtils.equals(payResult.getResultStatus(), "9000")) {
                    callbackContext.success(payResult.toJson());
                } else {
                    // 判断resultStatus 为非“9000”则代表可能支付失败
                    // “8000”代表支付结果因为支付渠道原因或者系统原因还在等待支付结果确认，最终交易是否成功以服务端异步通知为准（小概率状态）
                    if (TextUtils.equals(payResult.getResultStatus(), "8000")) {
                        callbackContext.success(payResult.toJson());
                    } else {
                        callbackContext.error(payResult.toJson());
                    }
                }*/
            }
        });
        this.cordova.setActivityResultCallback(this);
        /*
         Runnable payRunnable = new Runnable() {

            @Override
            public void run() {
                PayTask alipay = new PayTask(cordova.getActivity());
                Map<String, String> result = alipay.payV2(payInfo, true);
                Map<String , Object> obj = new HashMap<String, Object>();
                obj.put("result" ,result);
                obj.put("callback" ,callbackContext);
                Log.i("msp", result.toString());

                Message msg = new Message();
                msg.what = 1;
                msg.obj = obj;
                mHandler.sendMessage(msg);
            }
        };

        Thread payThread = new Thread(payRunnable);
        payThread.start();
        */
    }

    /**
     * create the order info. 创建订单信息
     */
    public static Map<String, String> buildOrderParamMap(String app_id,String tradeNo,String partner,String seller,String subject,String body,String total_fee) {
        Map<String, String> keyValues = new HashMap<String, String>();

        keyValues.put("app_id", app_id);

        keyValues.put("biz_content", "{\"timeout_express\":\"30m\",\"product_code\":\"QUICK_MSECURITY_PAY\",\"total_amount\":\"0.01\",\"subject\":\"1\",\"body\":\"123456\",\"out_trade_no\":\"" + tradeNo +  "\"}");

        keyValues.put("charset", "utf-8");

        keyValues.put("method", "alipay.trade.app.pay");

        keyValues.put("sign_type", "RSA");

        keyValues.put("timestamp", "2016-07-29 16:55:53");

        keyValues.put("version", "1.0");

        keyValues.put("notify_url","http://sys.online.sciencereading.cn/api/alipay/notify_url.php");


        return keyValues;
    }


    /**
     * sign the order info. 对订单信息进行签名
     *
     * @param content 待签名订单信息
     */
    public String sign(String content) {
        //return SignUtils.sign(content, privateKey);
        return "";
    }



    /**
     * 构造支付订单参数信息
     *
     * @param map
     * 支付订单参数
     * @return
     */
    public static String buildOrderParam(Map<String, String> map) {
        List<String> keys = new ArrayList<String>(map.keySet());

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < keys.size() - 1; i++) {
            String key = keys.get(i);
            String value = map.get(key);
            sb.append(buildKeyValue(key, value, true));
            sb.append("&");
        }

        String tailKey = keys.get(keys.size() - 1);
        String tailValue = map.get(tailKey);
        sb.append(buildKeyValue(tailKey, tailValue, true));

        return sb.toString();
    }


    /**
     * 拼接键值对
     *
     * @param key
     * @param value
     * @param isEncode
     * @return
     */
    private static String buildKeyValue(String key, String value, boolean isEncode) {
        StringBuilder sb = new StringBuilder();
        sb.append(key);
        sb.append("=");
        if (isEncode) {
            try {
                sb.append(URLEncoder.encode(value, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                sb.append(value);
            }
        } else {
            sb.append(value);
        }
        return sb.toString();
    }

    /**
     * 对支付参数信息进行签名
     *
     * @param map
     *            待签名授权信息
     *
     * @return
     */
    public static String getSign(Map<String, String> map, String rsaKey) {
        List<String> keys = new ArrayList<String>(map.keySet());
        // key排序
        Collections.sort(keys);

        StringBuilder authInfo = new StringBuilder();
        for (int i = 0; i < keys.size() - 1; i++) {
            String key = keys.get(i);
            String value = map.get(key);
            authInfo.append(buildKeyValue(key, value, false));
            authInfo.append("&");
        }

        String tailKey = keys.get(keys.size() - 1);
        String tailValue = map.get(tailKey);
        authInfo.append(buildKeyValue(tailKey, tailValue, false));

        //String oriSign = SignUtils.sign(authInfo.toString(), rsaKey);

        String oriSign="";

        String encodedSign = "";

        try {
            encodedSign = URLEncoder.encode(oriSign, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "sign=" + encodedSign;
    }
}
