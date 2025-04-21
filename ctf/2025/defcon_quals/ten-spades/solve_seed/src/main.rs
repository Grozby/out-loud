use indicatif::{ProgressBar, ProgressStyle};
use rayon::prelude::*;
use std::sync::Arc;
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use clap::Parser;

#[derive(Parser, Debug)]
#[command(author, version, about, long_about = None)]
struct Args {
    /// Seed state in hex (e.g., 0x13A81D49)
    #[arg(short, long)]
    seed_state: String,

    /// Expected cards as comma-separated numbers
    #[arg(short, long)]
    expected_cards: String,
}

struct Seed {
    multiplier: u32,
    offset: u32,
    modulus: u32,
    state: u32,
}

impl Seed {
    fn new(seed_multiplier: u32, seed_state: u32) -> Self {
        Self {
            multiplier: seed_multiplier,
            offset: 0x7E5,
            modulus: 0x7FFFFFFF,
            state: seed_state, // 0x13A81D49
        }
    }

    fn next(&mut self) -> u32 {
        self.state = ((self.state as u64 * self.multiplier as u64 + self.offset as u64)
            % (self.modulus as u64)) as u32;
        self.state
    }
}

fn parse_hex(hex_str: &str) -> u32 {
    if let Some(stripped) = hex_str.strip_prefix("0x") {
        u32::from_str_radix(stripped, 16).expect("Invalid hex number")
    } else {
        u32::from_str_radix(hex_str, 16).expect("Invalid hex number")
    }
}

fn main() {
    let args = Args::parse();
    // let expected_cards: [u32; 0x34] = [33, 49, 3, 51, 39, 37, 5, 48, 42, 7, 16, 47, 0, 40, 17, 44, 46, 23, 6, 41, 12, 21, 32, 10, 13, 22, 2, 34, 14, 25, 26, 27, 45, 28, 18, 4, 36, 24, 1, 35, 38, 50, 29, 19, 11, 9, 31, 30, 43, 15, 8, 20];
    // let expected_cards: [u32; 0x34] = [
    //     28, 23, 33, 49, 22, 31, 42, 24, 44, 0, 3, 40, 51, 20, 36, 38, 26, 2, 10, 45, 15, 1, 12, 50,
    //     30, 21, 11, 41, 25, 39, 29, 6, 48, 5, 19, 43, 9, 27, 17, 46, 34, 16, 35, 8, 18, 13, 47, 7,
    //     32, 37, 14, 4,
    // ];
    println!("Starting search...");
    
    // Parse seed state
    let seed_state = parse_hex(&args.seed_state);
    
    // Parse expected cards
    let expected_cards: Vec<u32> = args.expected_cards
        .split(',')
        .map(|s| s.trim().parse().expect("Invalid card number"))
        .collect();
    
    if expected_cards.len() != 0x34 {
        panic!("Expected exactly 0x34 cards, got {}", expected_cards.len());
    }
    
    // Convert to fixed-size array
    let expected_cards: [u32; 0x34] = expected_cards.try_into().unwrap();

    let pb = ProgressBar::new(0xffffffff);
    pb.set_style(
        ProgressStyle::default_bar()
            .template("[{elapsed_precise}] {bar:40.cyan/blue} {pos:>7}/{len:7} {msg} ({per_sec})")
            .unwrap()
            .progress_chars("=>-"),
    );

    let counter = Arc::new(AtomicU64::new(0));
    let found = Arc::new(AtomicBool::new(false));
    let counter_clone = counter.clone();
    let found_clone = found.clone();

    let chunk_size = 4_000_000;
    let chunks: Vec<_> = (0..0xffffffff as u64)
        .step_by(chunk_size as usize)
        .collect();

    let result = chunks.into_par_iter().find_map_any(|start| {
        let end = (start + chunk_size).min(0xffffffff);

        for i in start..end {
            if found_clone.load(Ordering::Relaxed) {
                return None;
            }

            let count = counter_clone.fetch_add(1, Ordering::Relaxed);
            if count % 1_000_000 == 0 {
                pb.set_position(count);
            }

            let mut seed = Seed::new(i as u32, seed_state);
            let cards = shuffle_cards(&mut seed);
            if cards == expected_cards {
                found_clone.store(true, Ordering::Relaxed);
                return Some(i);
            }
        }
        None
    });

    match result {
        Some(seed) => println!("\nFound seed: {:#x}", seed),
        None => println!("\nNo solution found!"),
    }

    pb.finish();
}

fn shuffle_cards(seed: &mut Seed) -> [u32; 0x34] {
    // Initialize cards with sequential numbers
    let mut cards: [u32; 0x34] = std::array::from_fn(|i| i as u32);

    let mut i: u32 = 0x33;
    while i > 0 {
        let j: u32 = (seed.next() & 0xffffffff) % (i + 1);
        let temp = cards[i as usize];
        cards[i as usize] = cards[j as usize];
        cards[j as usize] = temp;
        i -= 1;
    }
    cards
}
