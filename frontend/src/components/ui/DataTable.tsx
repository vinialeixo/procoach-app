import { ReactNode } from "react";
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
  onRowClick?: (row: T) => void;
};

// Tabela de dados genérica, no padrão "ledger clínico" (sem kit de UI).
// Usada nas listagens (atletas, KB, etc.). Clique na linha é opcional.
export function DataTable<T>({
  columns,
  rows,
  rowKey,
  emptyMessage = "Nenhum registro encontrado.",
  onRowClick,
}: DataTableProps<T>) {
  if (rows.length === 0) {
    return <p className="pc-table__empty">{emptyMessage}</p>;
  }

  return (
    <div className="pc-table__wrapper">
      <table className="pc-table">
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
