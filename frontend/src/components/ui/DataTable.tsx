import { KeyboardEvent, ReactNode } from "react";
import "./DataTable.css";

export type Column<T> = {
  key: string;
  header: string;
  render: (row: T) => ReactNode;
};

type DataTableProps<T> = {
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  emptyMessage?: string;
  /** Conteúdo de estado vazio mais rico (ex.: mensagem + ação); tem precedência sobre emptyMessage. */
  empty?: ReactNode;
  /** Rótulo acessível da tabela. */
  caption?: string;
  onRowClick?: (row: T) => void;
};

// Tabela de dados genérica, no padrão "ledger clínico" (sem kit de UI).
// Usada nas listagens (atletas, KB, etc.). Clique na linha é opcional e,
// quando existe, a linha também é operável por teclado (Enter/Espaço).
export function DataTable<T>({
  columns,
  rows,
  rowKey,
  emptyMessage = "Nenhum registro encontrado.",
  empty,
  caption,
  onRowClick,
}: DataTableProps<T>) {
  if (rows.length === 0) {
    return empty ? (
      <div className="pc-table__empty">{empty}</div>
    ) : (
      <p className="pc-table__empty">{emptyMessage}</p>
    );
  }

  function handleKeyDown(event: KeyboardEvent<HTMLTableRowElement>, row: T) {
    if (event.target !== event.currentTarget) return;
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      onRowClick?.(row);
    }
  }

  return (
    <div className="pc-table__wrapper">
      <table className="pc-table" aria-label={caption}>
        <thead>
          <tr>
            {columns.map((col) => (
              <th key={col.key} scope="col">
                {col.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr
              key={rowKey(row)}
              className={onRowClick ? "pc-table__row--clickable" : undefined}
              onClick={onRowClick ? () => onRowClick(row) : undefined}
              onKeyDown={onRowClick ? (event) => handleKeyDown(event, row) : undefined}
              tabIndex={onRowClick ? 0 : undefined}
            >
              {columns.map((col) => (
                <td key={col.key}>{col.render(row)}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
