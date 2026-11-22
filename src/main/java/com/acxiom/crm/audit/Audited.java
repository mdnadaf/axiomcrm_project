package com.acxiom.crm.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method for audit logging.
 *
 * Usage:
 * <pre>
 *   {@literal @}Audited(action = "CREATE", entity = "Customer")
 *   public CustomerDto createCustomer(...) { ... }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /** The action being performed, e.g. CREATE, UPDATE, DELETE, LOGIN, LOGOUT. */
    String action();

    /** The entity type being acted upon, e.g. "Customer", "Lead". */
    String entity();
}
