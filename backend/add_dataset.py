import pandas as pd
import os
import sys
from io import StringIO

# Import necessary functions from our existing backend
from model import load_encrypted_dataset, train_model, DATA_FILE, ENCRYPTED_DATA_FILE
from utils import encrypt_file

def add_new_data(new_csv_paths):
    print("1. Loading existing encrypted dataset...")
    try:
        df_existing = load_encrypted_dataset()
        # Keep only the original columns to merge properly
        if 'label' in df_existing.columns and 'text' in df_existing.columns:
            df_existing = df_existing[['label', 'text']]
    except Exception as e:
        print(f"Error loading existing data: {e}")
        return
        
    all_new_dataframes = []
    
    for path in new_csv_paths:
        print(f"2. Loading new dataset from {path}...")
        try:
            # Try UTF-8 first, fallback to Latin-1 for files like file7.csv
            try:
                df_new = pd.read_csv(path, encoding='utf-8')
            except UnicodeDecodeError:
                df_new = pd.read_csv(path, encoding='latin1')
            
            # Map column names to lowercase for easier matching
            df_new.columns = [str(c).lower().strip() for c in df_new.columns]
            
            # Auto-detect text column
            text_col = None
            for c in ['text', 'message', 'description', 'content', 'v2', 'url']:
                if c in df_new.columns:
                    text_col = c
                    break
                    
            # Auto-detect label column
            label_col = None
            for c in ['label', 'category', 'type', 'class', 'v1']:
                if c in df_new.columns:
                    label_col = c
                    break
            
            # Ensure we found columns
            if not text_col or not label_col:
                print(f"  -> Error: {path} has unknown columns: {list(df_new.columns)}. Skipping.")
                continue
            
            # Standardize column names
            df_new['text'] = df_new[text_col].astype(str)
            
            # Standardize labels (convert 'ham', '0', 'safe' -> safe, everything else -> phishing)
            def map_label(val):
                v = str(val).lower()
                if v in ['ham', 'safe', '0', 'false', 'legit', 'legitimate', 'normal']:
                    return 'safe'
                return 'phishing'
                
            df_new['label'] = df_new[label_col].apply(map_label)
            
            # Keep only the two required columns
            df_new = df_new[['label', 'text']]
            
            all_new_dataframes.append(df_new)
            print(f"  -> Successfully loaded and mapped {len(df_new)} rows from {path} (Using '{label_col}' and '{text_col}').")
        except Exception as e:
            print(f"  -> Failed to read {path}: {e}")
            continue
            
    if not all_new_dataframes:
        print("No valid new datasets were loaded. Aborting.")
        return

    print("3. Merging datasets...")
    # Combine the existing dataset with all the new datasets
    df_combined = pd.concat([df_existing] + all_new_dataframes, ignore_index=True)
    
    # Remove any duplicate rows to keep the AI training clean
    df_combined = df_combined.drop_duplicates()
    
    # Save the combined dataset to a temporary plaintext file
    df_combined.to_csv(DATA_FILE, index=False)
    
    print("4. Encrypting the new combined dataset securely...")
    encrypt_file(DATA_FILE, ENCRYPTED_DATA_FILE)
    
    # Delete the plaintext file so it isn't exposed
    if os.path.exists(DATA_FILE):
        os.remove(DATA_FILE)
        
    print(f"✅ Successfully merged! Total training examples is now: {len(df_combined)}")
    
    print("5. Retraining the AI model with the new data... (this might take a few seconds)")
    train_model()
    
if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python add_dataset.py <file1.csv> <file2.csv> ...")
        print("Example: python add_dataset.py dataset1.csv dataset2.csv dataset3.csv")
    else:
        # Pass all arguments (except the script name) to the function
        add_new_data(sys.argv[1:])
