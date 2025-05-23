package com.dynatrace.hash4j.hashing

class HashValue128(val mostSignificantBits: Long, val leastSignificantBits: Long) {
  val asInt: Int
    get() = this.leastSignificantBits.toInt()

  val asLong: Long
    get() = this.leastSignificantBits

  override fun hashCode(): Int = this.asInt

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null) {
      return false
    }
    if (javaClass != other.javaClass) {
      return false
    }
    other as HashValue128
    if (leastSignificantBits != other.leastSignificantBits) {
      return false
    }
    if (mostSignificantBits != other.mostSignificantBits) {
      return false
    }
    return true
  }

  override fun toString(): String = "HashValue128(leastSignificantBits=$leastSignificantBits, mostSignificantBits=$mostSignificantBits)"

  fun toByteArray(): ByteArray = throw UnsupportedOperationException()
}
