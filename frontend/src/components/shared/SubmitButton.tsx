import type { ComponentProps, ReactNode } from "react";
import { LoaderCircle } from "lucide-react";
import { Button } from "@/components/ui/button";

type SubmitButtonProps = Omit<ComponentProps<typeof Button>, "children" | "asChild"> & {
  pending: boolean;
  children: ReactNode;
};

/**
 * Standard submit button with loading indicator and accessible busy state.
 * Adheres to FE_CODE_CONVENTION.md Section 7.4.
 */
export function SubmitButton({
  pending,
  disabled,
  children,
  type = "submit",
  ...props
}: SubmitButtonProps) {
  return (
    <Button
      {...props}
      type={type}
      disabled={disabled || pending}
      aria-busy={pending}
    >
      {pending && (
        <LoaderCircle
          aria-hidden="true"
          className="mr-2 h-4 w-4 animate-spin"
        />
      )}
      {children}
    </Button>
  );
}
