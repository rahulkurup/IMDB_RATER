package com.imdbrater.application;

import org.codehaus.jackson.map.MapperConfig;
import org.codehaus.jackson.map.PropertyNamingStrategy;
import org.codehaus.jackson.map.introspect.AnnotatedField;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;

public class MyNameStrategy extends PropertyNamingStrategy
 {
  @Override
  public String nameForField(MapperConfig
                       config,
   AnnotatedField field, String defaultName) {
     return convert(defaultName);

  }
  @Override
  public String nameForGetterMethod(MapperConfig
                       config,
   AnnotatedMethod method, String defaultName) {
     return convert(defaultName);
  }

  @Override
  public String nameForSetterMethod(MapperConfig
                       config,
    AnnotatedMethod method, String defaultName) {
   String a = convert(defaultName); 
   return a;
  }

  public String convert(String defaultName )
  {
   char[] arr = defaultName.toCharArray();
   if(arr.length !=0)
   {
    if ( Character.isLowerCase(arr[0])){
     char upper = Character.toUpperCase(arr[0]);
     arr[0] = upper;
    }
   }
   return new StringBuilder().append(arr).toString();
  }

 }

