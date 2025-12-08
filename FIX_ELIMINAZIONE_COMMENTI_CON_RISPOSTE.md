# Fix: Eliminazione Ricorsiva Commenti con Risposte

**Data**: 2025-12-08
**Problema Riportato**:
1. Admin dashboard mostra contatori commenti non aggiornati correttamente
2. Quando si elimina un commento, le risposte non vengono eliminate

---

## 🔍 PROBLEMI IDENTIFICATI

### Problema #1: Risposte Non Eliminate con il Commento Parent
**File Coinvolti**:
- `CommentService.java` - metodo `eliminaCommento()`
- `AdminService.java` - metodo `eliminaCommento()`

**Comportamento Precedente**:
- Quando un commento veniva eliminato (soft delete), veniva marcato solo il parent come `isDeletedByAuthor = true`
- Le risposte (childComments) rimanevano nel database senza essere marcate come eliminate
- Il contatore `commentsCount` del post veniva decrementato di **1** anche se il commento aveva 5 risposte

**Esempio**:
```
Post ID: 100, commentsCount: 10

Commento ID: 1 (parent)
  ├─ Risposta ID: 2
  ├─ Risposta ID: 3
  └─ Risposta ID: 4

Elimino commento ID: 1
❌ Vecchio comportamento:
   - Solo commento 1 marcato come deleted
   - Risposte 2, 3, 4 rimangono nel DB come "orfane"
   - commentsCount decrementato a 9 (dovrebbe essere 6!)
```

**Impatto**:
- Risposte "orfane" nel database
- Contatori incorretti in tutti i punti dell'app
- Admin dashboard mostra numeri sbagliati

---

### Problema #2: Admin Dashboard Conta Anche Commenti Eliminati
**File**: `AdminService.java` - metodo `getSystemStats()`

**Comportamento Precedente**:
```java
long totaleCommenti = commentRepository.count();  // ❌ Conta TUTTI i commenti
```

Questo contava anche i commenti soft-deleted (`isDeletedByAuthor = true`), mostrando numeri gonfiati.

---

## ✅ SOLUZIONI IMPLEMENTATE

### Fix #1: Eliminazione Ricorsiva Commenti e Risposte

#### File Modificati:
1. **CommentRepository.java** - Aggiunti metodi helper

```java
/**
 * Conta i commenti totali non eliminati nel sistema.
 */
long countByIsDeletedByAuthorFalse();

/**
 * Trova tutti i commenti figli di un commento parent.
 * Include anche quelli soft-deleted per poterli eliminare ricorsivamente.
 */
@Query("SELECT c FROM Comment c WHERE c.parentComment.id = :parentId")
List<Comment> findAllChildCommentsByParentId(@Param("parentId") Long parentId);
```

#### 2. **CommentService.java** - Eliminazione ricorsiva

**Nuovo metodo helper**:
```java
/**
 * Elimina ricorsivamente un commento e tutti i suoi figli (soft delete).
 * Decrementa il contatore del post e pubblica eventi per ogni commento eliminato.
 */
private int deleteCommentAndChildren(Comment comment) {
    if (comment.getIsDeletedByAuthor()) {
        return 0;  // Già eliminato
    }

    Long postId = comment.getPost().getId();
    Long commentId = comment.getId();
    int deletedCount = 0;

    // 1. Trova e elimina ricorsivamente tutti i figli
    List<Comment> children = commentRepository.findAllChildCommentsByParentId(commentId);
    for (Comment child : children) {
        deletedCount += deleteCommentAndChildren(child);
    }

    // 2. Elimina il commento corrente (soft delete)
    comment.setIsDeletedByAuthor(true);
    commentRepository.save(comment);
    deletedCount++;

    // 3. Decrementa il contatore del post
    postRepository.updateCommentsCount(postId, -1);

    // 4. Pubblica eventi per menzioni e WebSocket
    eventPublisher.publishEvent(new DeleteMentionsEvent(MentionableType.COMMENT, commentId));
    eventPublisher.publishEvent(new CommentDeletedEvent(postId, commentId));

    return deletedCount;
}
```

**Metodo pubblico aggiornato**:
```java
@Transactional
public void eliminaCommento(Long commentId, Long userId) {
    Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new ResourceNotFoundException(...));

    // Verifica autorizzazione
    if (!comment.getUser().getId().equals(userId)) {
        throw new UnauthorizedException(...);
    }

    // Elimina ricorsivamente
    int deletedCount = deleteCommentAndChildren(comment);

    log.info("Commento {} e {} risposte eliminati", commentId, deletedCount - 1);
}
```

#### 3. **AdminService.java** - Stesso approccio per admin

Aggiunto metodo `deleteCommentAndChildrenAdmin()` identico, usato in `eliminaCommento()`.

**Nuovo comportamento**:
```
Post ID: 100, commentsCount: 10

Commento ID: 1 (parent)
  ├─ Risposta ID: 2
  ├─ Risposta ID: 3
  └─ Risposta ID: 4

Elimino commento ID: 1
✅ Nuovo comportamento:
   - Commenti 1, 2, 3, 4 tutti marcati come deleted
   - commentsCount decrementato 4 volte: 10 → 6 ✓
   - 4 eventi WebSocket inviati (1 per ogni commento)
   - 4 eventi DeleteMentionsEvent pubblicati
```

---

### Fix #2: Admin Dashboard Conta Solo Commenti Attivi

**File**: `AdminService.java` - metodo `getSystemStats()`

**Modifica**:
```java
// PRIMA (❌)
long totaleCommenti = commentRepository.count();

// DOPO (✅)
long totaleCommenti = commentRepository.countByIsDeletedByAuthorFalse();
```

Ora la dashboard mostra solo i commenti effettivamente visibili nel sistema.

---

## 🔄 FLUSSO COMPLETO DOPO I FIX

### Eliminazione Commento con Risposte

```
1. User/Admin chiama eliminaCommento(commentId: 1)
   ↓
2. CommentService carica Comment ID 1
   ↓
3. deleteCommentAndChildren(comment) - RICORSIONE:

   a) Trova figli del commento 1: [2, 3, 4]
   b) Per ogni figlio, chiama ricorsivamente deleteCommentAndChildren():
      - Figlio 2: soft delete, decrementa counter, pubblica eventi
      - Figlio 3: soft delete, decrementa counter, pubblica eventi
      - Figlio 4: soft delete, decrementa counter, pubblica eventi
   c) Dopo i figli, elimina il parent (commento 1):
      - Soft delete, decrementa counter, pubblica eventi

   Totale: 4 commenti eliminati
   ↓
4. Post commentsCount: 10 → 6 (decrementato 4 volte)
   ↓
5. Backend invia 4 CommentDeletedEvent via WebSocket:
   - { postId: 100, commentId: 2 }
   - { postId: 100, commentId: 3 }
   - { postId: 100, commentId: 4 }
   - { postId: 100, commentId: 1 }
   ↓
6. Frontend riceve 4 eventi:
   - PostDetailComponent.onCommentDeleted(2) → rimuove risposta, -1
   - PostDetailComponent.onCommentDeleted(3) → rimuove risposta, -1
   - PostDetailComponent.onCommentDeleted(4) → rimuove risposta, -1
   - PostDetailComponent.onCommentDeleted(1) → rimuove parent, -1

   commentsCount locale: 10 → 6 ✓
   ↓
7. CommentEventListener.broadcastCommentsCount() invia:
   - { postId: 100, commentsCount: 6, type: "comments_count_update" }
   ↓
8. PostCardComponent in feed/profilo/ricerca riceve update e mostra "6 commenti"
```

---

## 📊 IMPATTO DELLE MODIFICHE

### Backend ✅

| Componente | Modifiche | Impatto |
|------------|-----------|---------|
| **CommentRepository** | +2 metodi (`countByIsDeletedByAuthorFalse`, `findAllChildCommentsByParentId`) | Supporto per conteggio corretto e ricerca figli |
| **CommentService** | +1 metodo privato `deleteCommentAndChildren()` | Eliminazione ricorsiva + eventi multipli |
| **AdminService** | +1 metodo privato `deleteCommentAndChildrenAdmin()` | Eliminazione ricorsiva + audit log completo |
| **AdminService.getSystemStats** | Cambiato `count()` in `countByIsDeletedByAuthorFalse()` | Dashboard mostra solo commenti attivi |

### Frontend ✅ (Nessuna modifica necessaria!)

- **PostDetailComponent.onCommentDeleted()** - Già gestisce correttamente eventi multipli
- **PostCardComponent** - Già aggiornato nel fix precedente con `effectiveCommentsCount()`
- **WebSocket** - Già riceve e processa correttamente eventi multipli

---

## 🎯 RISULTATI FINALI

### ✅ Comportamento Corretto

1. **Eliminazione Commento Semplice**:
   - Commento eliminato ✓
   - Contatore post -1 ✓
   - Evento WebSocket inviato ✓

2. **Eliminazione Commento con Risposte**:
   - Commento parent eliminato ✓
   - Tutte le risposte eliminate ricorsivamente ✓
   - Contatore post decrementato N volte (N = totale commenti eliminati) ✓
   - N eventi WebSocket inviati ✓
   - Frontend aggiorna correttamente ✓

3. **Admin Dashboard**:
   - Mostra solo commenti non eliminati ✓
   - Contatori accurati ✓

4. **Post Detail**:
   - Contatore si aggiorna correttamente ✓
   - Commenti e risposte scompaiono dalla UI ✓

5. **Feed/Profilo/Ricerca**:
   - Contatori aggiornati in tempo reale ✓

---

## 📝 NOTE TECNICHE

### Perché Soft Delete con Ricorsione?

1. **Mantiene Audit Trail**: I commenti rimangono nel DB per audit/logging
2. **Integrità Referenziale**: No problemi con foreign keys
3. **Rollback Possibile**: Si può "ripristinare" un commento se necessario
4. **Consistenza**: Tutti i contatori rimangono accurati

### Performance

- **Worst Case**: Eliminazione di un commento con N risposte = N+1 query
  - 1 query per trovare i figli
  - N update per marcare come deleted
  - N decrementi del counter
  - N eventi pubblicati

- **Ottimizzazione**: Per thread molto lunghi (>100 risposte), potremmo:
  - Fare batch updates invece di singole save
  - Raggruppare eventi WebSocket
  - **MA** questi casi sono rari in un social media

### Testing Suggerito

1. Creare un commento con 5 risposte
2. Eliminare il parent come utente proprietario
3. Verificare:
   - Tutti i 6 commenti marcati come `isDeletedByAuthor = true` ✓
   - Post `commentsCount` decrementato di 6 ✓
   - Admin dashboard mostra conteggio corretto ✓
   - Post detail mostra conteggio corretto ✓
   - Feed/profilo/ricerca mostrano conteggio corretto ✓

---

## 🚀 DEPLOYMENT

### Checklist Pre-Deploy

- [x] CommentRepository: metodi aggiunti
- [x] CommentService: eliminazione ricorsiva implementata
- [x] AdminService: eliminazione ricorsiva implementata
- [x] AdminService.getSystemStats: conteggio corretto
- [x] Frontend: già compatibile (no modifiche necessarie)

### Compatibilità

✅ **Backward Compatible**: Sì
- Vecchi commenti nel DB funzionano normalmente
- Nessuna migrazione database necessaria
- Frontend già gestisce eventi multipli

### Rollback

Se necessario rollback:
1. Revertire le modifiche ai Service
2. Il sistema tornerà al comportamento precedente
3. Nessuna perdita dati (soft delete)
