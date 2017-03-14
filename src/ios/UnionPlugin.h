#import <Cordova/CDV.h>
#import "UPPayPluginDelegate.h"

@interface UnionPayPlugin : CDVPlugin

@property(nonatomic,strong)NSString *partner;
@property(nonatomic,strong)NSString *seller;
@property(nonatomic,strong)NSString *privateKey;
@property(nonatomic,strong)NSString *currentCallbackId;



- (void) pay:(CDVInvokedUrlCommand*)command;
@end
