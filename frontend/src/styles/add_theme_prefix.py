#!/usr/bin/env python3
"""
Add [data-theme="xxx"] prefixes to CSS selectors in specific line ranges of app.css.

Uses a state-machine CSS parser to correctly identify selectors vs properties.
"""

import re

FILE_PATH = r"e:\图书馆书籍定位系统\frontend\src\styles\app.css"

# Define regions: (start_line_1indexed, end_line_1indexed_inclusive, theme)
# The file has 4088 lines total.
REGIONS = [
    (2268, 2917, "midnight-archive"),
    (2919, 3303, "bamboo-monastery"),
    (3305, 3888, "bamboo-monastery"),
    (3890, 4088, "bamboo-monastery"),
]


def is_in_region(line_num_1indexed):
    """Return the theme for a given 1-indexed line number, or None."""
    for start, end, theme in REGIONS:
        if start <= line_num_1indexed <= end:
            return theme
    return None


def prefix_single_selector(sel, theme):
    """Add [data-theme="xxx"] prefix to a single CSS selector."""
    sel = sel.strip()
    if not sel:
        return sel

    prefix = f'[data-theme="{theme}"] '

    # Already prefixed
    if sel.startswith('[data-theme='):
        return sel

    # Pseudo-elements / pseudo-classes at root: ::selection, ::-webkit-scrollbar
    if sel.startswith('::'):
        return prefix + sel

    # body (possibly with ::pseudo)
    if sel == 'body' or sel.startswith('body::') or sel.startswith('body:'):
        return prefix + sel

    # .scholarly-app...
    if sel.startswith('.scholarly-app'):
        return prefix + sel

    # HTML tags that appear bare in the file: textarea, input, button, select, etc.
    # Also tag with pseudo/combinator: textarea:focus, etc.
    html_tags = [
        'a', 'article', 'aside', 'b', 'blockquote', 'body', 'br', 'button',
        'code', 'dd', 'div', 'dl', 'dt', 'em', 'fieldset', 'footer', 'form',
        'h1', 'h2', 'h3', 'h4', 'h5', 'h6', 'header', 'hr', 'html', 'i',
        'iframe', 'img', 'input', 'label', 'legend', 'li', 'link', 'main',
        'mark', 'meta', 'nav', 'ol', 'option', 'p', 'pre', 'section',
        'select', 'small', 'span', 'strong', 'style', 'sub', 'summary', 'sup',
        'table', 'tbody', 'td', 'textarea', 'tfoot', 'th', 'thead', 'time',
        'tr', 'ul', 'video',
    ]
    for tag in html_tags:
        if sel == tag:
            return prefix + sel
        # tag immediately followed by . # : [ :: > + ~ combinator or pseudo
        if re.match(r'^' + re.escape(tag) + r'([\.#:\[\s>+~,])', sel):
            return prefix + sel

    # Classes and IDs
    if sel.startswith('.') or sel.startswith('#'):
        return prefix + sel

    # :is(), :where(), :not(), :root etc.
    if sel.startswith(':'):
        return prefix + sel

    # Universal selector
    if sel.startswith('*'):
        return prefix + sel

    # @keyframes, @media etc - should not reach here but just in case
    if sel.startswith('@'):
        return sel

    # Fallback: prepend prefix
    return prefix + sel


def prefix_selector_list(selector_text, theme):
    """
    Process a comma-separated list of selectors.
    Splits by top-level commas (not inside parens/brackets).
    """
    parts = split_selectors(selector_text)

    prefixed = []
    for part in parts:
        stripped = part.strip()
        if stripped:
            prefixed.append(prefix_single_selector(stripped, theme))
        # Skip empty parts

    return ',\n'.join(prefixed)


def split_selectors(text):
    """Split selector text by commas, respecting parentheses and brackets."""
    parts = []
    depth = 0
    current = []
    for ch in text:
        if ch in ('(', '['):
            depth += 1
            current.append(ch)
        elif ch in (')', ']'):
            depth = max(0, depth - 1)
            current.append(ch)
        elif ch == ',' and depth == 0:
            parts.append(''.join(current))
            current = []
        else:
            current.append(ch)
    if current:
        parts.append(''.join(current))
    return parts


def parse_and_process(lines):
    """
    Parse the CSS file as a sequence of tokens (selectors, properties, comments, etc.)
    and add theme prefixes to selectors in the designated regions.

    We use a simple approach: iterate through lines and track brace depth.
    When brace_depth is 0 (or at @media level), a non-property line is a selector.
    """
    result = []
    line_count = len(lines)

    # State tracking
    brace_depth = 0  # Track nesting of {}
    in_selector = False  # Are we currently collecting selector text?
    selector_start = None  # Index where current selector started
    selector_lines = []  # Accumulated selector lines

    # To handle @media properly, we track the media brace depth
    media_depth = 0  # How many @media blocks deep are we?

    i = 0
    while i < line_count:
        line_num = i + 1  # 1-indexed
        line = lines[i]
        stripped = line.strip()
        theme = is_in_region(line_num)

        # Skip lines not in any themed region
        if not theme:
            result.append(line)
            # Still track brace depth for correctness
            brace_depth += count_braces(stripped)
            i += 1
            continue

        # We're in a themed region

        # Skip empty lines
        if not stripped:
            result.append(line)
            i += 1
            continue

        # Skip block comments (/* ... */)
        if stripped.startswith('/*'):
            # Check if comment ends on same line
            if '*/' in stripped:
                result.append(line)
                i += 1
                continue
            # Multi-line comment
            result.append(line)
            i += 1
            while i < line_count and '*/' not in lines[i]:
                result.append(lines[i])
                i += 1
            if i < line_count:
                result.append(lines[i])
                i += 1
            continue

        # Skip lines starting with * (continuation of block comment)
        if stripped.startswith('*') and stripped.endswith('*/'):
            result.append(line)
            i += 1
            continue

        # @media rules: don't prefix the @media line itself
        if stripped.startswith('@media'):
            result.append(line)
            i += 1
            continue

        # @keyframes etc: skip entirely
        if stripped.startswith('@') and not stripped.startswith('@media'):
            result.append(line)
            i += 1
            continue

        # Closing brace
        if stripped == '}':
            result.append(line)
            brace_depth = max(0, brace_depth - 1)
            i += 1
            continue

        # If brace_depth indicates we're inside a property block,
        # check if this is a property or a nested selector
        # Properties are indented, selectors may or may not be (inside @media)

        # Check: is this a CSS property line?
        if is_property_line(stripped):
            result.append(line)
            i += 1
            continue

        # Otherwise, this is a selector line.
        # Collect all lines until we find {
        selector_text_parts = []
        selector_indices = []
        brace_found = False
        j = i

        while j < line_count:
            s_line = lines[j].strip()
            theme_j = is_in_region(j + 1)

            # If outside themed region, stop collecting
            if not theme_j:
                break

            # Skip empty lines within selector collection
            if not s_line:
                j += 1
                continue

            # Stop if we hit a comment
            if s_line.startswith('/*'):
                break

            # Stop if we hit @media
            if s_line.startswith('@media'):
                break

            selector_indices.append(j)
            selector_text_parts.append(s_line)

            if '{' in s_line:
                brace_found = True
                break

            if '}' in s_line:
                # Shouldn't happen in selector, but stop
                break

            j += 1

        if not brace_found or not selector_indices:
            # Didn't find a complete rule, output as-is
            result.append(line)
            i += 1
            continue

        # We have a selector! Process it.
        # The last line may have both selector text and {
        last_idx = selector_indices[-1]
        last_stripped = lines[last_idx].strip()

        brace_pos = last_stripped.index('{')
        last_selector_part = last_stripped[:brace_pos].rstrip()

        # Build the full selector text
        if len(selector_indices) == 1:
            full_selector = last_selector_part
        else:
            parts = []
            for k, idx in enumerate(selector_indices):
                if k < len(selector_indices) - 1:
                    parts.append(lines[idx].strip())
                else:
                    parts.append(last_selector_part)
            full_selector = '\n'.join(parts)

        # Prefix the selector
        prefixed = prefix_selector_list(full_selector, theme)
        prefixed_parts = prefixed.split('\n')

        # Reconstruct lines
        # Determine original indentation of first selector line
        first_line = lines[selector_indices[0]]
        base_indent = get_indent(first_line)

        if len(selector_indices) == 1:
            # Single selector line
            result.append(base_indent + prefixed_parts[0] + ' {\n')
        else:
            # Multi-line selector
            for k in range(max(len(selector_indices), len(prefixed_parts))):
                if k < len(prefixed_parts):
                    orig_indent = get_indent(lines[selector_indices[k]]) if k < len(selector_indices) else base_indent
                    if k == len(selector_indices) - 1:
                        result.append(orig_indent + prefixed_parts[k] + ' {\n')
                    elif k < len(selector_indices):
                        result.append(orig_indent + prefixed_parts[k] + '\n')
                    else:
                        result.append(orig_indent + prefixed_parts[k] + '\n')

        i = last_idx + 1

    return result


def get_indent(line):
    """Get the leading whitespace of a line."""
    return line[:len(line) - len(line.lstrip())]


def count_braces(line):
    """Count net braces ({ minus }) in a line, respecting strings and comments."""
    count = 0
    in_string = False
    i = 0
    while i < len(line):
        ch = line[i]
        if ch in ('"', "'"):
            in_string = not in_string
        elif not in_string:
            if ch == '{':
                count += 1
            elif ch == '}':
                count -= 1
        i += 1
    return count


def is_property_line(stripped):
    """
    Determine if a stripped line is a CSS property declaration.

    Properties look like: property-name: value;
    Selectors look like: .class, .class:hover, body::before, etc.
    """
    # If it contains { it's likely a selector+brace
    if '{' in stripped:
        return False

    # If it's just } it's a closing brace
    if stripped == '}':
        return False

    # If it starts with @ it's an at-rule
    if stripped.startswith('@'):
        return False

    # If it starts with // it's a comment
    if stripped.startswith('//'):
        return False

    # If it starts with /* or * it's a comment
    if stripped.startswith('/*') or (stripped.startswith('*') and stripped.endswith('*/')):
        return False

    # Match CSS property pattern: starts with a property name followed by :
    # CSS property names consist of lowercase letters, digits, and hyphens
    # They always start with a letter (not . or # or : etc.)
    prop_match = re.match(r'^([a-zA-Z][a-zA-Z0-9-]*)\s*:', stripped)
    if prop_match:
        prop_name = prop_match.group(1)
        # Make sure it's not a selector that happens to match
        # Selectors that could match: body:hover doesn't match because : comes right after
        # But "body:" would match - however body doesn't have a colon property
        # Check if what follows the colon looks like a CSS value (not a pseudo-class pattern)

        # After the property name + colon, the rest should be the value
        # If the line has something like "color: red;" that's a property
        # If the line has "body:hover" - the colon is part of pseudo-class

        # Most CSS properties are well-known, so let's check against a comprehensive list
        # Plus anything with - in it is likely a property
        if '-' in prop_name:
            return True

        known_props = {
            'color', 'background', 'border', 'padding', 'margin', 'font',
            'display', 'position', 'top', 'bottom', 'left', 'right',
            'width', 'height', 'overflow', 'opacity', 'z', 'cursor', 'visibility',
            'box', 'text', 'filter', 'backdrop', 'transition', 'animation',
            'transform', 'content', 'pointer', 'isolation', 'mask',
            'accent', 'outline', 'letter', 'line', 'word', 'white',
            'vertical', 'box', 'fill', 'stroke', 'clip', 'appearance',
            'scroll', 'resize', 'user', 'table', 'counter',
            'writing', 'direction', 'object', 'aspect', 'container',
            'gap', 'flex', 'grid', 'align', 'justify', 'place',
            'inset', 'list',
        }

        for kp in known_props:
            if prop_name == kp or prop_name.startswith(kp + '-'):
                return True

        # Additional common properties
        extra_props = {
            'color', 'background', 'border', 'border-radius', 'border-color',
            'border-top', 'border-bottom', 'border-left', 'border-right',
            'border-style', 'border-width', 'border-collapse', 'border-spacing',
            'padding', 'padding-top', 'padding-bottom', 'padding-left', 'padding-right',
            'margin', 'margin-top', 'margin-bottom', 'margin-left', 'margin-right',
            'font', 'font-family', 'font-size', 'font-weight', 'font-style',
            'text', 'text-align', 'text-decoration', 'text-indent', 'text-transform',
            'text-overflow', 'text-shadow',
            'background-color', 'background-image', 'background-size',
            'min-height', 'max-height', 'min-width', 'max-width',
            'line-height', 'letter-spacing', 'word-break', 'white-space',
            'display', 'position', 'top', 'bottom', 'left', 'right',
            'width', 'height', 'overflow', 'overflow-x', 'overflow-y',
            'opacity', 'visibility', 'z-index', 'cursor',
            'box-shadow', 'box-sizing',
            'filter', 'backdrop-filter', '-webkit-backdrop-filter',
            'transition', 'animation', 'transform',
            'content', 'pointer-events', 'isolation', 'mask-image',
            'color-scheme', 'accent-color', 'outline', 'outline-offset',
            'flex', 'flex-wrap', 'flex-direction', 'flex-grow', 'flex-shrink', 'flex-basis',
            'grid', 'grid-template-columns', 'grid-template-rows', 'grid-column', 'grid-row',
            'gap', 'row-gap', 'column-gap',
            'align-items', 'align-self', 'align-content',
            'justify-content', 'justify-self', 'justify-items',
            'place-items', 'place-content', 'place-self',
            'border-radius', 'border-top-left-radius', 'border-top-right-radius',
            'border-bottom-left-radius', 'border-bottom-right-radius',
            'inset', 'float', 'clear',
            'list-style', 'resize', 'user-select',
            'table-layout', 'caption-side',
            'counter-increment', 'counter-reset',
            'writing-mode', 'direction',
            'object-fit', 'object-position',
            'aspect-ratio', 'container-type',
            '-webkit-mask-image', 'clip-path', 'appearance',
            'scroll-behavior',
            'fill', 'stroke',
        }

        if prop_name in extra_props:
            return True

    return False


def main():
    with open(FILE_PATH, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    print(f"Read {len(lines)} lines from {FILE_PATH}")

    result = parse_and_process(lines)

    with open(FILE_PATH, 'w', encoding='utf-8') as f:
        f.writelines(result)

    print(f"Wrote {len(result)} lines back to {FILE_PATH}")
    print("Done!")


if __name__ == '__main__':
    main()
