import { Validators } from '@angular/forms';

// Hibernate Validator 8 implements @NotBlank with Java String.trim(), which strips characters up to U+0020.
export const nonBlankText = Validators.pattern(/[\u0021-\uFFFF]/);
