<template>
  <div>
    <header class="app-header">
      <RouterLink class="brand" to="/barmaker/commandes">Le Bar'app</RouterLink>
      <nav aria-label="Navigation barmaker">
        <RouterLink to="/barmaker/commandes">Commandes</RouterLink>
        <RouterLink to="/barmaker/categories">Catégories</RouterLink>
        <RouterLink to="/barmaker/cocktails">Cocktails</RouterLink>
      </nav>
    </header>
    <main class="page-shell"><RouterView /></main>
  </div>
</template>
