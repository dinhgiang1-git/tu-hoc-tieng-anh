import { AlertCircle, RefreshCw } from "lucide-react";
import { Button } from "@/components/ui/button";

interface ErrorStateProps {
  message: string;
  traceId?: string;
  onRetry?: () => void;
}

export function ErrorState({ message, traceId, onRetry }: ErrorStateProps) {
  return (
    <div className="flex flex-col items-center justify-center p-8 text-center rounded-lg border border-destructive/20 bg-destructive/5 my-4">
      <AlertCircle className="h-10 w-10 text-destructive mb-3" />
      <h3 className="text-lg font-semibold text-foreground mb-1">
        Đã xảy ra lỗi
      </h3>
      <p className="text-sm text-muted-foreground max-w-md mb-4">{message}</p>
      {traceId && (
        <p className="text-xs font-mono text-muted-foreground/70 mb-4">
          Mã tra cứu: {traceId}
        </p>
      )}
      {onRetry && (
        <Button variant="outline" size="sm" onClick={onRetry}>
          <RefreshCw className="mr-2 h-4 w-4" />
          Thử lại
        </Button>
      )}
    </div>
  );
}
