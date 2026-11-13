import { getAthleteId } from "@/lib/auth";
import {
  buildShoppingList,
  computeDerivedMacros,
  FOOD_STATE_LABEL,
  Meal,
  OBJECTIVE_LABEL,
  ShoppingListItem,
  useCurrentAthleteDiet,
} from "@/lib/diets";
import { AthleteShell } from "@/layouts/AthleteShell";
import "./AthleteContent.css";

function RequestError({ status }: { status?: number }) {
  if (status === 401) {
    return (
      <p role="alert">
        Sua sessão expirou. Entre novamente para consultar sua dieta.
      </p>
    );
  }
  if (status === 403) {
    return (
      <p role="alert">
        Você não tem permissão para consultar este conteúdo.
      </p>
    );
  }
  return (
    <p role="alert">
      Não foi possível carregar a dieta. Tente novamente mais tarde.
    </p>
  );
}

function MealCard({ meal }: { meal: Meal }) {
  const foodItems = meal.items.filter((item) => item.kind === "FOOD");

  return (
    <article className="athlete-content__card athlete-content__meal">
      <header className="athlete-content__meal-header">
        <div className="athlete-content__meal-title">
          <h3>{meal.name}</h3>
          {meal.time && (
            <span className="athlete-content__meal-time">{meal.time}</span>
          )}
        </div>
        {meal.totals && (
          <p className="athlete-content__meal-totals">
            {meal.totals.calories.toLocaleString("pt-BR")} kcal · P{" "}
            {meal.totals.protein}g · C {meal.totals.carbohydrates}g · G{" "}
            {meal.totals.fat}g
          </p>
        )}
      </header>

      {meal.notes && <p className="athlete-content__hint">{meal.notes}</p>}

      {foodItems.length > 0 && (
        <ul className="athlete-content__meal-items">
          {foodItems.map((item) => (
            <li key={item.id} className="athlete-content__meal-item">
              <div className="athlete-content__meal-item-main">
                <strong>{item.name}</strong>
                <span className="athlete-content__meal-qty">
                  {item.quantityG} g
                </span>
                <span className="athlete-content__badge">
                  {FOOD_STATE_LABEL[item.state]}
                </span>
              </div>
              {(item.householdMeasure || item.preparation) && (
                <p className="athlete-content__meal-item-meta">
                  {[item.householdMeasure, item.preparation]
                    .filter(Boolean)
                    .join(" · ")}
                </p>
              )}
            </li>
          ))}
        </ul>
      )}
    </article>
  );
}

function ShoppingListCard({
  headingId,
  title,
  items,
}: {
  headingId: string;
  title: string;
  items: ShoppingListItem[];
}) {
  if (items.length === 0) return null;

  return (
    <section className="athlete-content__card" aria-labelledby={headingId}>
      <h2 id={headingId}>{title}</h2>
      <ul className="athlete-content__shopping-list">
        {items.map((item) => (
          <li key={item.key} className="athlete-content__shopping-item">
            <div className="athlete-content__meal-item-main">
              <strong>{item.name}</strong>
              <span className="athlete-content__meal-qty">
                {item.totalQuantityG} g
              </span>
              <span className="athlete-content__badge">
                {FOOD_STATE_LABEL[item.state]}
              </span>
            </div>
            {item.householdMeasures.length > 0 && (
              <p className="athlete-content__meal-item-meta">
                {item.householdMeasures.join(" · ")}
              </p>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}

export function AthleteDietPage() {
  const athleteId = getAthleteId();
  const query = useCurrentAthleteDiet(athleteId ?? "");
  const status =
    query.error && typeof query.error === "object" && "response" in query.error
      ? (query.error as { response?: { status?: number } }).response?.status
      : undefined;

  const diet = query.data;
  const derived = diet ? computeDerivedMacros(diet.macros) : null;
  const sortedMeals = diet
    ? [...diet.meals].sort((a, b) => a.order - b.order)
    : [];
  const shoppingList =
    diet && diet.meals.length > 0 ? buildShoppingList(diet.meals) : null;

  return (
    <AthleteShell>
      <section className="athlete-content">
        <h1>Minha dieta</h1>
        {!athleteId ? (
          <p role="alert">
            Não foi possível identificar sua conta. Entre novamente.
          </p>
        ) : query.isPending ? (
          <p role="status">Carregando sua dieta…</p>
        ) : query.isError ? (
          <RequestError status={status} />
        ) : !diet || !derived ? (
          <p>Ainda não há uma dieta aprovada disponível.</p>
        ) : (
          <>
            <article className="athlete-content__card">
              <p className="athlete-content__eyebrow">
                Plano alimentar · versão {diet.version}
              </p>
              <h2>{OBJECTIVE_LABEL[diet.objective]}</h2>
              <dl className="athlete-content__metrics">
                <div>
                  <dt>Calorias</dt>
                  <dd>{diet.macros.calories.toLocaleString("pt-BR")} kcal</dd>
                </div>
                <div>
                  <dt>Proteínas</dt>
                  <dd>
                    {diet.macros.protein} g{" "}
                    <span className="athlete-content__macro-pct">
                      ({Math.round(derived.proteinPct)}% kcal)
                    </span>
                  </dd>
                </div>
                <div>
                  <dt>Carboidratos</dt>
                  <dd>
                    {diet.macros.carbohydrates} g{" "}
                    <span className="athlete-content__macro-pct">
                      ({Math.round(derived.carbsPct)}% kcal)
                    </span>
                  </dd>
                </div>
                <div>
                  <dt>Gorduras</dt>
                  <dd>
                    {diet.macros.fat} g{" "}
                    <span className="athlete-content__macro-pct">
                      ({Math.round(derived.fatPct)}% kcal)
                    </span>
                  </dd>
                </div>
              </dl>
            </article>

            {sortedMeals.length > 0 && (
              <>
                <section
                  className="athlete-content__diet-section"
                  aria-labelledby="meals-heading"
                >
                  <h2 id="meals-heading">Refeições</h2>
                  <div className="athlete-content__meals">
                    {sortedMeals.map((meal) => (
                      <MealCard key={meal.id} meal={meal} />
                    ))}
                  </div>
                </section>

                {shoppingList && (
                  <>
                    <ShoppingListCard
                      headingId="supplements-heading"
                      title="Suplementos"
                      items={shoppingList.supplements}
                    />
                    <ShoppingListCard
                      headingId="shopping-list-heading"
                      title="Lista de compras"
                      items={shoppingList.foods}
                    />
                  </>
                )}
              </>
            )}
          </>
        )}
      </section>
    </AthleteShell>
  );
}
