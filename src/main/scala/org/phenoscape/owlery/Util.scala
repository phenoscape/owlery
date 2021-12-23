package org.phenoscape.owlery

import com.google.common.base.Optional

object Util {

  implicit class OptionalOption[T](val self: Optional[T]) extends AnyVal {

    def asScala: Option[T] = if (self.isPresent) Some(self.get) else None

  }

}
