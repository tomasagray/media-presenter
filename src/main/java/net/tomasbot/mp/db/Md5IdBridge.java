package net.tomasbot.mp.db;

import net.tomasbot.mp.model.Md5Id;
import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.springframework.stereotype.Component;

@Component
public class Md5IdBridge implements IdentifierBridge<Md5Id> {

  @Override
  public String toDocumentIdentifier(
      Md5Id propertyValue, IdentifierBridgeToDocumentIdentifierContext context) {
    return propertyValue == null ? null : propertyValue.getHashId();
  }

  @Override
  public Md5Id fromDocumentIdentifier(
      String documentIdentifier, IdentifierBridgeFromDocumentIdentifierContext context) {
    return new Md5Id(documentIdentifier);
  }
}
