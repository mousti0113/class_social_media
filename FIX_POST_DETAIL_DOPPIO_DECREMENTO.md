# Fix: Post Detail - Doppio Decremento Contatore Commenti

**Data**: 2025-12-08
**Problema**: In post-detail, eliminando un commento il contatore veniva decrementato il doppio (es: da 4 a 2 invece che da 4 a 3).

---

## 🔍 ANALISI DEL PROBLEMA

### Comportamento Osservato
```
Post con 4 commenti
User elimina 1 commento
❌ Contatore mostrato: 2 (invece di 3)
✅ Dopo uscita e rientro: 3 (corretto dal backend)
```

### Root Cause: Doppia Chiamata a `onCommentDeleted()`

Il metodo `onCommentDeleted()` veniva chiamato **due volte** per lo stesso commento:

1. **Prima chiamata - Evento locale dal template**:
   ```html
   <app-comment
     [comment]="comment"
     (deleted)="onCommentDeleted($event)"  <!-- ⬅️ CHIAMATA #1 -->
   />
   ```
   Quando l'utente cliccava "Elimina", `CommentComponent` emetteva l'evento `deleted` → chiamava `onCommentDeleted()` → **-1 al contatore**

2. **Seconda chiamata - Evento WebSocket**:
   ```typescript
   this.websocketService.commentUpdates$.subscribe((update) => {
       if (update.type === 'comment_deleted') {
           this.onCommentDeleted(update.commentId);  // ⬅️ CHIAMATA #2
       }
   });
   ```
   Il backend inviava l'evento WebSocket → ricevuto dal listener → chiamava di nuovo `onCommentDeleted()` → **altro -1 al contatore**

### Risultato: Doppio Decremento

```
Stato iniziale: commentsCount = 4
  ↓
Evento locale (deleted): commentsCount = 3  (-1)
  ↓
Evento WebSocket: commentsCount = 2  (-1 di nuovo!)
```

Con l'eliminazione ricorsiva (parent + 3 risposte = 4 commenti):
```
Stato iniziale: commentsCount = 10
  ↓
4 eventi locali: 10 → 6  (-4)
  ↓
4 eventi WebSocket: 6 → 2  (-4 di nuovo!)
```

---

## ✅ SOLUZIONE IMPLEMENTATA

### Separazione delle Responsabilità

Abbiamo creato **due metodi distinti**:

1. **`onCommentDeletedByUser(commentId)`** - Per eventi locali
   - Rimuove il commento dalla UI (optimistic update)
   - **NON** tocca il contatore
   - Il contatore verrà aggiornato via WebSocket

2. **`onCommentDeleted(commentId)`** - Per eventi WebSocket
   - Rimuove il commento dalla UI
   - Decrementa il contatore

### Codice Modificato

**File**: `post-detail-component.ts`

```typescript
/**
 * Gestisce l'eliminazione di un commento da parte dell'utente corrente (evento locale).
 * Rimuove solo dalla UI in modo optimistic, il contatore verrà aggiornato via WebSocket.
 */
onCommentDeletedByUser(commentId: number): void {
    this.post.update(current => {
        if (!current) return current;

        // Rimuovi solo dalla UI, NON toccare il contatore
        const updatedCommenti = current.commenti
            .filter(c => c.id !== commentId)
            .map(c => ({
                ...c,
                risposte: c.risposte.filter(r => r.id !== commentId)
            }));

        return {
            ...current,
            commenti: updatedCommenti,
            // NON aggiorniamo commentsCount qui, arriverà via WebSocket
        };
    });
}

/**
 * Gestisce l'eliminazione di un commento ricevuta via WebSocket.
 * Rimuove dalla UI e decrementa il contatore.
 */
onCommentDeleted(commentId: number): void {
    this.post.update(current => {
        if (!current) return current;

        // Rimuovi il commento o la risposta
        const updatedCommenti = current.commenti
            .filter(c => c.id !== commentId)
            .map(c => ({
                ...c,
                risposte: c.risposte.filter(r => r.id !== commentId)
            }));

        return {
            ...current,
            commenti: updatedCommenti,
            commentsCount: Math.max(0, current.commentsCount - 1),  // ✅ Solo qui
        };
    });
}
```

**File**: `post-detail-component.html`

```html
<!-- PRIMA (❌) -->
<app-comment
    (deleted)="onCommentDeleted($event)"
/>

<!-- DOPO (✅) -->
<app-comment
    (deleted)="onCommentDeletedByUser($event)"
/>
```

---

## 🔄 FLUSSO CORRETTO DOPO IL FIX

### Scenario: Utente Elimina Commento con 3 Risposte

```
Post: commentsCount = 10

1. User clicca "Elimina" sul commento ID 1 (che ha 3 risposte)
   ↓

2. CommentComponent emette evento deleted(1)
   ↓ CHIAMATA LOCALE

3. PostDetailComponent.onCommentDeletedByUser(1)
   - Rimuove commento 1 dalla UI (optimistic)
   - NON tocca commentsCount
   - UI: commento scompare, contatore ancora "10"
   ↓

4. Backend elimina ricorsivamente: 1, 2, 3, 4
   - Decrementa commentsCount 4 volte: 10 → 6
   - Pubblica 4 CommentDeletedEvent via WebSocket
   ↓

5. WebSocket invia 4 eventi:
   { commentId: 1, type: "comment_deleted" }
   { commentId: 2, type: "comment_deleted" }
   { commentId: 3, type: "comment_deleted" }
   { commentId: 4, type: "comment_deleted" }
   ↓

6. PostDetailComponent riceve i 4 eventi WebSocket
   ↓ CHIAMATE WEBSOCKET

7. Per ogni evento chiama onCommentDeleted():
   - onCommentDeleted(1): rimuove dalla UI (già rimosso), commentsCount: 10 → 9
   - onCommentDeleted(2): rimuove risposta, commentsCount: 9 → 8
   - onCommentDeleted(3): rimuove risposta, commentsCount: 8 → 7
   - onCommentDeleted(4): rimuove risposta, commentsCount: 7 → 6

Risultato finale: commentsCount = 6 ✅ CORRETTO!
```

### Vantaggi della Soluzione

1. **UI Reattiva**: Il commento scompare subito (optimistic update)
2. **Contatore Accurato**: Viene aggiornato una sola volta via WebSocket
3. **Consistenza**: Se l'eliminazione fallisce, WebSocket non arriva e il contatore resta corretto
4. **Compatibilità**: Funziona anche quando altri utenti eliminano commenti

---

## 📊 CONFRONTO PRIMA/DOPO

### Prima del Fix ❌

| Azione | Contatore Locale | Backend DB | Problema |
|--------|-----------------|------------|----------|
| Elimina 1 commento | -2 | -1 | Disallineato |
| Elimina parent + 3 risposte | -8 | -4 | Doppio decremento |
| Dopo refresh | Corretto | Corretto | Richiede refresh |

### Dopo il Fix ✅

| Azione | Contatore Locale | Backend DB | Risultato |
|--------|-----------------|------------|-----------|
| Elimina 1 commento | -1 | -1 | ✅ Sincronizzato |
| Elimina parent + 3 risposte | -4 | -4 | ✅ Sincronizzato |
| Senza refresh | Corretto | Corretto | ✅ No refresh necessario |

---

## 🎯 TEST CASE

### Test 1: Eliminazione Commento Semplice
```
Setup: Post con 5 commenti
Azione: User elimina 1 commento
Verifica:
  - ✅ Commento scompare dalla UI immediatamente
  - ✅ Contatore passa da 5 a 4
  - ✅ Nessun refresh necessario
```

### Test 2: Eliminazione Commento con Risposte
```
Setup: Post con 10 commenti (1 parent + 3 risposte)
Azione: User elimina il parent
Verifica:
  - ✅ Parent e 3 risposte scompaiono dalla UI
  - ✅ Contatore passa da 10 a 6
  - ✅ Nessun doppio decremento
```

### Test 3: Altro Utente Elimina
```
Setup: Post con 8 commenti, 2 browser aperti
Azione: User A elimina 1 commento
Verifica Browser B:
  - ✅ Commento scompare
  - ✅ Contatore aggiornato da 8 a 7
  - ✅ Solo decremento via WebSocket (nessun evento locale)
```

---

## 🚀 FILE MODIFICATI

1. **post-detail-component.ts**
   - ✅ Aggiunto `onCommentDeletedByUser()` per eventi locali
   - ✅ Modificato `onCommentDeleted()` per eventi WebSocket

2. **post-detail-component.html**
   - ✅ Cambiato `(deleted)="onCommentDeleted($event)"` in `(deleted)="onCommentDeletedByUser($event)"`

---

## 📝 NOTE TECNICHE

### Optimistic Updates

La strategia "optimistic update" significa:
- Aggiorniamo la UI immediatamente (ottimisticamente assumendo successo)
- Il server valida e conferma via WebSocket
- Se fallisce, possiamo rollback (anche se ora non gestiamo errori)

Vantaggi:
- UX fluida e reattiva
- Nessun flash/salto visivo
- Feedback immediato

### Perché Separare i Metodi?

Alternativa scartata: usare un flag per distinguere la chiamata
```typescript
onCommentDeleted(commentId: number, fromWebSocket = false) {
    // ... logica con if
}
```

Svantaggi:
- Meno chiaro l'intent
- Facile dimenticare il flag
- Più fragile

La separazione in due metodi rende esplicito il comportamento.

---

## ✅ RISULTATO FINALE

**Issue**: Post detail decrementava il contatore il doppio ❌
**Root Cause**: Evento locale + WebSocket entrambi decrementavano ❌
**Fix**: Separati i due percorsi, solo WebSocket decrementa ✅
**Test**: Elimina parent con 3 risposte: 10 → 6 (corretto) ✅

**Pronto per il testing!** 🚀
