package com.duncpro.autochess

import java.util.stream.Stream
import kotlin.properties.Delegates
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

inline fun <reified T> implementClassEquality(other: Any?, equalityCheck: (T) -> Boolean): Boolean {
    if (other == null) return false
    if (other::class != T::class) return false
    return equalityCheck(other as T)
}

inline fun <reified T> implementInterfaceEquality(other: Any?, equalityCheck: (T) -> Boolean): Boolean {
    if (other !is  T) return false
    return equalityCheck(other)
}

enum class Direction(val signum: Int) { HIGHER(1), LOWER(-1) }

fun <T, R> Stream<T>.mapNotNull(mapper: (T) -> R?): Stream<R> {
    return this.map(mapper)
        .filter { it != null }
        .map {
            @Suppress("UNCHECKED_CAST")
            it as R
        }
}

fun <T> Stream<T?>.filterNotNull(): Stream<T> {
    return this.filter { it != null }
        .map { it!! }
}

fun <T, C: Collection<T>> C.only(): T? {
    if (this.size > 1) throw IllegalStateException()
    return this.firstOrNull()
}

fun <T> List<T>.distinctOrThrow(): HashSet<T> {
    val set = HashSet<T>()
    this.forEach(set::add)
    if (set.size != this.size) throw IllegalStateException()
    return set
}

inline fun <reified R> Stream<out Any>.filterIsInstance(): Stream<R> = this
    .filter { x -> x is R }
    .map { x -> x as R }


class CachedProperty<T, V>(val getCurrentVersionId: () -> Int, val compute: () -> V) : ReadOnlyProperty<T, V> {
    private var cachedVersionId: Int? = null
    private var cachedVersion: V? = null

    override fun getValue(thisRef: T, property: KProperty<*>): V {
        val currentVersionId = getCurrentVersionId()
        if (cachedVersionId != currentVersionId) {
            cachedVersion = compute()
            cachedVersionId = currentVersionId

        }
        return cachedVersion!!
    }
}