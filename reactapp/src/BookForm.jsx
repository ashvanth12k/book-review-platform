import React from 'react';

/**
 * BookForm — shared for Add and Edit book.
 *
 * Props:
 *  - formData      : { title, author, genre, description, publisherEmail? }
 *  - onChange      : standard change handler
 *  - onSubmit      : form submit handler
 *  - submitLabel   : button label (default "Add Book")
 *  - isAdmin       : boolean — when true, shows publisher dropdown
 *  - publishers    : array of { id, username, email } (admin only)
 *  - isEditing     : boolean — hides publisher dropdown in edit mode
 */
export default function BookForm({
  formData,
  onChange,
  onSubmit,
  submitLabel = 'Add Book',
  isAdmin = false,
  publishers = [],
  isEditing = false,
}) {
  return (
    <form onSubmit={onSubmit}>
      <div className="form-grid">
        <div className="form-group">
          <input
            type="text"
            name="title"
            placeholder="Title"
            value={formData.title}
            onChange={onChange}
          />
        </div>
        <div className="form-group">
          <input
            type="text"
            name="author"
            placeholder="Author"
            value={formData.author}
            onChange={onChange}
          />
        </div>
        <div className="form-group">
          <input
            type="text"
            name="genre"
            placeholder="Genre"
            value={formData.genre}
            onChange={onChange}
          />
        </div>
        <div className="form-group">
          <input
            type="text"
            name="description"
            placeholder="Description"
            value={formData.description}
            onChange={onChange}
          />
        </div>

        {/* Publisher dropdown — admin Add mode only (hidden during edit) */}
        {isAdmin && !isEditing && (
          <div className="form-group" style={{ gridColumn: '1 / -1' }}>
            <select
              name="publisherEmail"
              value={formData.publisherEmail || ''}
              onChange={onChange}
              style={{
                width: '100%',
                padding: '0.5rem 0.75rem',
                borderRadius: '6px',
                border: '1px solid var(--border, #d1d5db)',
                fontSize: '0.95rem',
                color: formData.publisherEmail ? 'inherit' : '#9ca3af',
                background: 'var(--input-bg, #fff)',
              }}
            >
              <option value="">— Assign to Publisher (optional) —</option>
              {publishers.map((p) => (
                <option key={p.id} value={p.email}>
                  {p.username} ({p.email})
                </option>
              ))}
            </select>
          </div>
        )}
      </div>
      <button type="submit" className="btn">{submitLabel}</button>
    </form>
  );
}
