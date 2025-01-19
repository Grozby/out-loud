package com.beust.jcommander.converters;

import com.beust.jcommander.IStringConverter;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class InetAddressConverter implements IStringConverter<InetAddress> {
   public InetAddress convert(String host) {
      try {
         return InetAddress.getByName(host);
      } catch (UnknownHostException var3) {
         throw new IllegalArgumentException(host, var3);
      }
   }
}
