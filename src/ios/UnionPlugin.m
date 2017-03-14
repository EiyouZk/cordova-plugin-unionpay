#import "UnionPlugin.h"
#import "UPPaymentControl.h"
#import "MainViewController.h"
#import "AppDelegate.h"


#import "DataSigner.h"
#import <AlipaySDK/AlipaySDK.h>
#import "UPPayPlugin.h"

@implementation UnionPayPlugin

#define kVCTitle          @"商户测试"
#define kBtnFirstTitle    @"获取订单，开始测试"
#define kWaiting          @"正在获取TN,请稍后..."
#define kNote             @"提示"
#define kConfirm          @"确定"
#define kErrorNet         @"网络错误"
#define kResult           @"支付结果：%@"

-(void)pluginInitialize{
    CDVViewController *viewController = (CDVViewController *)self.viewController;
    self.partner = [viewController.settings objectForKey:@"partner"];
    self.seller = [viewController.settings objectForKey:@"seller"];
    self.privateKey = [viewController.settings objectForKey:@"privatekey"];
}

- (void) pay:(CDVInvokedUrlCommand*)command
{
    self.currentCallbackId = command.callbackId;
    /*
     *商户的唯一的parnter和seller。
     *签约后，支付宝会为每个商户分配一个唯一的 parnter 和 seller。
     */
    
    //partner和seller获取失败,提示
    if ([self.partner length] == 0 ||
        [self.seller length] == 0 ||
        [self.privateKey length] == 0)
    {
        UIAlertView *alert = [[UIAlertView alloc] initWithTitle:@"提示"
                                                        message:@"缺少partner或者seller或者私钥。"
                                                       delegate:self
                                              cancelButtonTitle:@"确定"
                                              otherButtonTitles:nil];
        [alert show];
        return;
    }
    
    /*
     *生成订单信息及签名
     */
    
    //从API请求获取支付信息
    NSMutableDictionary *args = [command argumentAtIndex:0];
    NSString   *tn    = [args objectForKey:@"tn"];
    
//    MainViewController *controller = (MainViewController *)self.viewController;
//    
//    
//    //调用银联支付接口
//    [UPPayPlugin startPay:tn mode:@"01" viewController:(MainViewController *)self delegate:(id<UPPayPluginDelegate>)self];
    
    NSString   *tnMde = @"00";
    MainViewController *controller = (MainViewController *)self.viewController;
    if (tn != nil&& tn.length>0)
    {
        [[UPPaymentControl defaultControl]
         startPay:tn
         fromScheme:@"UPPayDemo"
         mode:tnMde
         viewController:controller
         ];
    }

}

#pragma mark UPPayPluginResult
- (void)UPPayPluginResult:(NSString *)result
{
    NSString* msg = [NSString stringWithFormat:kResult, result];
    
//        if (true) {
//            [self successWithCallbackID:self.currentCallbackId messageAsDictionary:msg];
//        } else {
//            [self failWithCallbackID:self.currentCallbackId messageAsDictionary:msg];
//        }
    //[self showAlertMessage:msg];
}
- (void)handlePaymentResult:(NSURL*)url completeBlock:(UPPaymentResultBlock)completionBlock
{
    
    //结果code为成功时，先校验签名，校验成功后做后续处理
    NSLog(@"xxx");
}


- (void)handleOpenURL:(NSNotification *)notification
{
    NSURL* url = [notification object];
    
    if ([url isKindOfClass:[NSURL class]] && [url.scheme isEqualToString:@"UPPayDemo"])
    {
        
            [[UPPaymentControl defaultControl] handlePaymentResult:url completeBlock:^(NSString *code, NSDictionary *data) {
        
                NSString *MEMO=@"memo";
                NSString *STATUS=@"resultStatus";
                NSString *result=@"result";
                
                //结果code为成功时，先校验签名，校验成功后做后续处理
                if([code isEqualToString:@"success"]) {
        
                    //判断签名数据是否存在
                    if(data == nil){
                        //如果没有签名数据，建议商户app后台查询交易结果
                        return;
                    }
        
                    //数据从NSDictionary转换为NSString
                    NSData *signData = [NSJSONSerialization dataWithJSONObject:data
                                                                       options:0
                                                                         error:nil];
                    
                    
                    NSString *sign = [[NSString alloc] initWithData:signData encoding:NSUTF8StringEncoding];
                    
                    
                    NSMutableDictionary *dict=[NSMutableDictionary dictionaryWithObjectsAndKeys:
                                               @"memo", MEMO, @"true", STATUS, @"支付完成", result, sign, @"resultdate",  nil];
                    
                    
                    [self successWithCallbackID:self.currentCallbackId messageAsDictionary:dict];
                    //验签证书同后台验签证书
                    //此处的verify，商户需送去商户后台做验签
                    //if([self verify:sign]) {
                        //支付成功且验签成功，展示支付成功提示
                    //}
                    //else {
                        //验签失败，交易结果数据被篡改，商户app后台查询交易结果
                    //}
                }
                else if([code isEqualToString:@"fail"]) {
                    //交易失败
                    NSMutableDictionary *dict=[NSMutableDictionary dictionaryWithObjectsAndKeys:
                                               @"memo", MEMO, @"false", STATUS, @"支付失败", result,@"fail",@"resultdate",  nil];
                    
                    [self failWithCallbackID:self.currentCallbackId messageAsDictionary:dict];
                }
                else if([code isEqualToString:@"cancel"]) {
                    //交易取消
                    NSMutableDictionary *dict=[NSMutableDictionary dictionaryWithObjectsAndKeys:
                                               @"memo", MEMO, @"false", STATUS, @"用户取消操作", result,@"cancel",@"resultdate",  nil];
                    
                    [self failWithCallbackID:self.currentCallbackId messageAsDictionary:dict];
                }
            }];
        
    }
}

- (void)successWithCallbackID:(NSString *)callbackID withMessage:(NSString *)message
{
    CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:message];
    [self.commandDelegate sendPluginResult:commandResult callbackId:callbackID];
}

- (void)failWithCallbackID:(NSString *)callbackID withMessage:(NSString *)message
{
    CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:message];
    [self.commandDelegate sendPluginResult:commandResult callbackId:callbackID];
}
- (void)successWithCallbackID:(NSString *)callbackID messageAsDictionary:(NSDictionary *)message
{
    CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:message];
    [self.commandDelegate sendPluginResult:commandResult callbackId:callbackID];
}

- (void)failWithCallbackID:(NSString *)callbackID messageAsDictionary:(NSDictionary *)message
{
    CDVPluginResult *commandResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary:message];
    [self.commandDelegate sendPluginResult:commandResult callbackId:callbackID];
}



//-(BOOL) verifyLocal:(NSString *) resultStr {
//
//    //从NSString转化为NSDictionary
//    NSData *resultData = [resultStr dataUsingEncoding:NSUTF8StringEncoding];
//    NSDictionary *data = [NSJSONSerialization JSONObjectWithData:resultData options:0 error:nil];
//
//    //获取生成签名的数据
//    NSString *sign = data[@"sign"];
//    NSString *signElements = data[@"data"];
//    //NSString *pay_result = signElements[@"pay_result"];
//    //NSString *tn = signElements[@"tn"];
//    //转换服务器签名数据
//    NSData *nsdataFromBase64String = [[NSData alloc]
//                                      initWithBase64EncodedString:sign options:0];
//    //生成本地签名数据，并生成摘要
////    NSString *mySignBlock = [NSString stringWithFormat:@"pay_result=%@tn=%@",pay_result,tn];
//    NSData *dataOriginal = [[self sha1:signElements] dataUsingEncoding:NSUTF8StringEncoding];
//    //验证签名
//    //TODO：此处如果是正式环境需要换成public_product.key
//    NSString *pubkey =[self readPublicKey:@"public_test.key"];
//    OSStatus result=[RSA verifyData:dataOriginal sig:nsdataFromBase64String publicKey:pubkey];
//
//
//
//    //签名验证成功，商户app做后续处理
//    if(result == 0) {
//        //支付成功且验签成功，展示支付成功提示
//        return YES;
//    }
//    else {
//        //验签失败，交易结果数据被篡改，商户app后台查询交易结果
//        return NO;
//    }
//
//    return NO;
//}

//
//- (BOOL) openURL:(NSURL *)url sourceApplication:(NSString *)sourceApplication annotation:(id)annotation {
//    
//    
//    [[UPPaymentControl defaultControl] handlePaymentResult:url completeBlock:^(NSString *code, NSDictionary *data) {
//        
//        //结果code为成功时，先校验签名，校验成功后做后续处理
//        if([code isEqualToString:@"success"]) {
//            
//            //判断签名数据是否存在
//            if(data == nil){
//                //如果没有签名数据，建议商户app后台查询交易结果
//                return;
//            }
//            
//            //数据从NSDictionary转换为NSString
//            NSData *signData = [NSJSONSerialization dataWithJSONObject:data
//                                                               options:0
//                                                                 error:nil];
//            NSString *sign = [[NSString alloc] initWithData:signData encoding:NSUTF8StringEncoding];
//            
//            
//            
//            //验签证书同后台验签证书
//            //此处的verify，商户需送去商户后台做验签
//            //if([self verify:sign]) {
//                //支付成功且验签成功，展示支付成功提示
//            //}
//            //else {
//                //验签失败，交易结果数据被篡改，商户app后台查询交易结果
//            //}
//        }
//        else if([code isEqualToString:@"fail"]) {
//            //交易失败
//        }
//        else if([code isEqualToString:@"cancel"]) {
//            //交易取消
//        }
//    }];
//    
//    return YES;
//}
//
//
//// NOTE: 9.0以后使用新API接口
//- (BOOL)application:(UIApplication *)app openURL:(NSURL *)url options:(NSDictionary<NSString*, id> *)options
//{
//    [[UPPaymentControl defaultControl] handlePaymentResult:url completeBlock:^(NSString *code, NSDictionary *data) {
//        
//        //结果code为成功时，先校验签名，校验成功后做后续处理
//        if([code isEqualToString:@"success"]) {
//            
//            //判断签名数据是否存在
//            if(data == nil){
//                //如果没有签名数据，建议商户app后台查询交易结果
//                return;
//            }
//            
//            //数据从NSDictionary转换为NSString
//            NSData *signData = [NSJSONSerialization dataWithJSONObject:data
//                                                               options:0
//                                                                 error:nil];
//            NSString *sign = [[NSString alloc] initWithData:signData encoding:NSUTF8StringEncoding];
//            
//            
//            
//            //验签证书同后台验签证书
//            //此处的verify，商户需送去商户后台做验签
//            //if([self verify:sign]) {
//                //支付成功且验签成功，展示支付成功提示
//            //}
//            //else {
//                //验签失败，交易结果数据被篡改，商户app后台查询交易结果
//            //}
//        }
//        else if([code isEqualToString:@"fail"]) {
//            //交易失败
//        }
//        else if([code isEqualToString:@"cancel"]) {
//            //交易取消
//        }
//    }];
//    
//    return YES;
//}


- (NSString*)sha1:(NSString *)string
{
    //    unsigned char digest[CC_SHA1_DIGEST_LENGTH];
    //    CC_SHA1_CTX context;
    //    NSString *description;
    //
    //    CC_SHA1_Init(&context);
    //
    //    memset(digest, 0, sizeof(digest));
    //
    //    description = @"";
    //
    //
    //    if (string == nil)
    //    {
    //        return nil;
    //    }
    //
    //    // Convert the given 'NSString *' to 'const char *'.
    //    const char *str = [string cStringUsingEncoding:NSUTF8StringEncoding];
    //
    //    // Check if the conversion has succeeded.
    //    if (str == NULL)
    //    {
    //        return nil;
    //    }
    //
    //    // Get the length of the C-string.
    //    int len = (int)strlen(str);
    //
    //    if (len == 0)
    //    {
    //        return nil;
    //    }
    //
    //
    //    if (str == NULL)
    //    {
    //        return nil;
    //    }
    //
    //    CC_SHA1_Update(&context, str, len);
    //
    //    CC_SHA1_Final(digest, &context);
    //
    //    description = [NSString stringWithFormat:
    //                   @"%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x",
    //                   digest[ 0], digest[ 1], digest[ 2], digest[ 3],
    //                   digest[ 4], digest[ 5], digest[ 6], digest[ 7],
    //                   digest[ 8], digest[ 9], digest[10], digest[11],
    //                   digest[12], digest[13], digest[14], digest[15],
    //                   digest[16], digest[17], digest[18], digest[19]];
    
    //    return description;
    
    return @"";
}

@end
