# ISDD — Indian Scam Detection Dataset (v1.0)

First structured dataset for Indian-language phishing and scam detection (SMS, WhatsApp, Email).

**Curator:** Ayush More (VIT Pune)  
**License:** CC-BY-4.0 (academic research)

## Generate / refresh

From repo root:

```bash
pip install pandas scikit-learn
python scripts/generate_isdd_dataset.py --total 4000
```

For the full paper target (~8000 messages):

```bash
python scripts/generate_isdd_dataset.py --total 8000
```

## Layout

- `raw/` — JSON per channel + language
- `processed/` — `ISDD_combined_v1.0.csv`, train/test/val splits
- `annotations/scam_taxonomy.json`
- `utilities/` — loaders and validation

## Train Aegis model on ISDD (legacy SMS dataset unchanged)

```bash
cd backend
set DATASET_SOURCE=isdd
python -c "from model import train_model; train_model()"
```

Legacy encrypted data remains at `backend/dataset.csv.enc` when `DATASET_SOURCE=legacy` (default).

## Citation

See `2_isdd_dataset_schema_guidelines.md` section 9.
