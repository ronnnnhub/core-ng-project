package core.framework.internal.web.bean;

import core.framework.internal.bean.BeanClassValidator;
import core.framework.internal.json.JSONMapper;
import core.framework.internal.json.JSONWriter;
import core.framework.internal.validate.Validator;
import core.framework.internal.web.service.ErrorResponse;
import core.framework.internal.web.site.AJAXErrorResponse;
import core.framework.util.Maps;
import core.framework.util.Strings;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Optional;

/**
 * @author neo
 */
public class ResponseBeanWriter {   // used by controller and web service
    private final Map<Class<?>, Context<?>> context = Maps.newHashMap();

    public ResponseBeanWriter() {
        context.put(ErrorResponse.class, new Context<>(ErrorResponse.class));
        context.put(AJAXErrorResponse.class, new Context<>(AJAXErrorResponse.class));
    }

    public void register(Type responseType, BeanClassValidator validator) {
        Class<?> beanClass = ContextHelper.responseBeanClass(responseType);

        if (!context.containsKey(beanClass)) {
            validator.validate(beanClass);
            context.put(beanClass, new Context<>(beanClass));
        }
    }

    public boolean contains(Class<?> beanClass) {
        return context.containsKey(beanClass);
    }

    public byte[] toJSON(Object bean) {
        if (bean instanceof Optional) {  // only support Optional<T> as response bean type
            Optional<?> optional = (Optional<?>) bean;
            if (optional.isEmpty()) return Strings.bytes("null");
            Object value = optional.get();
            Context<Object> context = ContextHelper.context(this.context, value.getClass());
            context.validator.validate(value, false);
            return context.writer.toJSON(value);
        } else {
            Context<Object> context = ContextHelper.context(this.context, bean.getClass());
            context.validator.validate(bean, false);
            return context.writer.toJSON(bean);
        }
    }

    private static class Context<T> {
        final JSONWriter<T> writer;
        final Validator<T> validator;

        Context(Class<T> beanClass) {
            writer = JSONMapper.writer(beanClass);
            validator = Validator.of(beanClass);
        }
    }
}
