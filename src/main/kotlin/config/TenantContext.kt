package com.muditsahni.config

import com.muditsahni.model.entity.Tenant
import kotlinx.coroutines.ThreadContextElement
import org.springframework.stereotype.Component
import kotlin.coroutines.CoroutineContext
import kotlin.text.get
import kotlin.text.set


@Component
class TenantContext {
    object TenantContextKey {
        private val threadLocal = ThreadLocal<Tenant?>()

        fun get(): Tenant? = threadLocal.get()
        fun set(tenant: Tenant?) = threadLocal.set(tenant)
    }

    class TenantContextElement(private val tenant: Tenant) : ThreadContextElement<Tenant?> {
        companion object Key : CoroutineContext.Key<TenantContextElement>

        override val key: CoroutineContext.Key<TenantContextElement> = Key

        override fun updateThreadContext(context: CoroutineContext): Tenant? {
            val previousTenant = TenantContextKey.get()
            TenantContextKey.set(tenant)
            return previousTenant
        }

        override fun restoreThreadContext(context: CoroutineContext, oldState: Tenant?) {
            TenantContextKey.set(oldState)
        }
    }

    companion object {
        val logger = mu.KotlinLogging.logger {}


        private val tenantContextKey = TenantContextKey

        fun setTenant(tenant: Tenant): TenantContextElement {
            return TenantContextElement(tenant)
        }

        fun getTenant(): Tenant? {
            return tenantContextKey.get()
        }

        fun clear() {
            tenantContextKey.set(null)
        }

        fun getDatabaseName(tenantName: String? = null): String {
            val tenant = tenantName ?: getTenant()?.name ?: "default"
            return "tenant-$tenant-db"
        }
    }
}


