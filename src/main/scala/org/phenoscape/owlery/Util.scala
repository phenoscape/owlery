package org.phenoscape.owlery

import com.google.common.base.Optional

object Util {

  implicit class OptionalOption[T](val self: Optional[T]) extends AnyVal {

    def toOption: Option[T] = if (self.isPresent) Option(self.get) else None

  }

}
