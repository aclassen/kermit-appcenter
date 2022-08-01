// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

#if __has_include(<AppCenter/MSACServiceAbstract.h>)
#import <AppCenter/MSACServiceAbstract.h>
#else
#import "MSACServiceAbstract.h"
#endif

#import <Foundation/Foundation.h>

@class MSACExceptionModel;
@class MSACErrorAttachmentLog;

NS_SWIFT_NAME(Crashes)
@interface MSACCrashes : MSACServiceAbstract

/**
 * Track handled error.
 *
 * @param error error.
 * @param properties dictionary of properties.
 * @param attachments a list of attachments.
 *
 * @return handled error ID.
 */
+ (NSString *_Nonnull)trackError:(NSError *_Nonnull)error
                  withProperties:(nullable NSDictionary<NSString *, NSString *> *)properties
                     attachments:(nullable NSArray<MSACErrorAttachmentLog *> *)attachments NS_SWIFT_NAME(trackError(_:properties:attachments:));

/**
 * Track handled exception from custom exception model.
 *
 * @param exception custom model exception.
 * @param properties dictionary of properties.
 * @param attachments a list of attachments.
 *
 * @return handled error ID.
 */
+ (NSString *_Nonnull)trackException:(MSACExceptionModel *_Nonnull)exception
                      withProperties:(nullable NSDictionary<NSString *, NSString *> *)properties
                         attachments:(nullable NSArray<MSACErrorAttachmentLog *> *)attachments NS_SWIFT_NAME(trackException(_:properties:attachments:));

@end
