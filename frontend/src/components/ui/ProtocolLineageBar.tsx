import { ReactNode } from "react";
import "./ProtocolLineageBar.css";

type ProtocolLineageBarProps = {
  lineageTag?: string;
  title: string;
  metrics?: string[];
  action?: ReactNode;
};

export function ProtocolLineageBar({
  lineageTag,
  title,
  metrics,
  action,
}: ProtocolLineageBarProps) {
  return (
    <section className="pc-lineage-bar" aria-label="Contexto clínico do protocolo">
      <div className="pc-lineage-bar__left">
        {lineageTag && <span className="pc-lineage-bar__tag">{lineageTag}</span>}
        <div className="pc-lineage-bar__info">
          <h2 className="pc-lineage-bar__title">{title}</h2>
          {metrics && metrics.length > 0 && (
            <p className="pc-lineage-bar__metrics">{metrics.join(" · ")}</p>
          )}
        </div>
      </div>
      {action && <div className="pc-lineage-bar__action">{action}</div>}
    </section>
  );
}
