/*
 * RSAEncrypt.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.crypto;

import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.ExternalJavaScriptLoader.Callback;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.common.crypto.CryptoServerOperations.PublicKeyInfo;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;

public class RSAEncrypt
{
   public interface ResponseCallback
   {
      void onSuccess(String encryptedData);
      void onFailure(ServerError error);
   }
   
   
   public static void encrypt_ServerOnly(
         final CryptoServerOperations server,
         final String input,
         final ResponseCallback callback)
   {
      if (Desktop.isDesktop())
      {
         // Don't encrypt for desktop, Windows can't decrypt it.
         callback.onSuccess(input);
         return;
      }

      loader_.addCallback(new Callback()
      {
         @Override
         public void onLoaded()
         {
            if (publicKeyInfo_ == null)
            {
               server.getPublicKey(new ServerRequestCallback<PublicKeyInfo>()
               {
                  @Override
                  public void onResponseReceived(PublicKeyInfo response)
                  {
                     publicKeyInfo_ = response;
                     callback.onSuccess(encrypt(input,
                                                publicKeyInfo_.getExponent(),
                                                publicKeyInfo_.getModulo()));
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     callback.onFailure(error);
                  }
               });
            }
            else
            {
               callback.onSuccess(encrypt(input,
                                          publicKeyInfo_.getExponent(),
                                          publicKeyInfo_.getModulo()));
            }
         }
      });
   }
   
   public static void clearCache()
   {
      publicKeyInfo_ = null;
   }

   private static native String encrypt(String value,
                                        String exponent,
                                        String modulo) /*-{
      return $wnd.encrypt(value, exponent, modulo);
   }-*/;

   private static final ExternalJavaScriptLoader loader_ =
         new ExternalJavaScriptLoader("js/encrypt.min.js");
   private static PublicKeyInfo publicKeyInfo_;
}
