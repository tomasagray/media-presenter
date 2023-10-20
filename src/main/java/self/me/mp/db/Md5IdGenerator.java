package self.me.mp.db;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.springframework.stereotype.Component;
import self.me.mp.model.Md5Id;

@Component
public class Md5IdGenerator implements IdentifierGenerator {

	@Override
	public Object generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
		return object == null ? null : new Md5Id(object.toString());
	}
}
