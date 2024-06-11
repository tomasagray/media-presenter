package self.me.mp.db;

import org.hibernate.search.mapper.pojo.bridge.IdentifierBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeFromDocumentIdentifierContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.IdentifierBridgeToDocumentIdentifierContext;
import org.springframework.stereotype.Component;
import self.me.mp.model.Md5Id;

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
